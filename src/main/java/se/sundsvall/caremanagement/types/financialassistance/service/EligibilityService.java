package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseSummary;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ApplicationSuggestion;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUGS;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;

/**
 * Application-eligibility (common entry point) routing for financial assistance, encoding the agreed decision flow:
 *
 * <ol>
 * <li><b>Protected identity</b> — a safety gate ahead of the routing: if the applicant or co-applicant has protected
 * identity in population register (citizen) or Lifecare, no application is offered (empty suggestions). The response
 * carries
 * <em>no</em> reason or flag — the protected status must not leak across the API edge — so the frontend simply sees
 * that nothing can be recommended and directs the citizen to a caseworker.</li>
 * <li><b>Finns i CM? + LC</b> — does the applicant already exist (an EB errand in caremanagement, or a Lifecare
 * footprint)? When applying together, <em>both</em> must exist. If not → new application.</li>
 * <li><b>Samma marital status?</b> — does the requested constellation (alone vs with a partner, inferred from the
 * co-applicant) match the previous application's? If it changed → new application.</li>
 * <li><b>Per-month</b> — for the current and next month, is there already an application/decision (within the window)?
 * If yes → supplementary application for that month; if no → renewal. The current month being decided in Lifecare makes
 * the
 * next-month option the recommended one.</li>
 * </ol>
 *
 * The decision is advisory — the caseworker stays in the loop — so the result carries the facts each gate was decided
 * from and degrades ({@code lifecareChecked=false}) when Lifecare is unreachable.
 */
@Service
@Transactional(readOnly = true)
public class EligibilityService {

	static final String REASON_NO_EXISTING_CASE = "NO_EXISTING_CASE";
	static final String REASON_MARITAL_STATUS_CHANGED = "MARITAL_STATUS_CHANGED";
	static final String REASON_RECENTLY_CLOSED = "RECENTLY_CLOSED";
	static final String REASON_EXISTING_CASE = "EXISTING_CASE";
	static final String REASON_ALL_TYPES_TEST = "ALL_TYPES_TEST";

	private static final String[] MONTHS_EN = {
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	};

	private final ErrandRepository errandRepository;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final LifecareEbCaseService lifecareEbCaseService;
	private final CitizenService citizenService;
	private final RecentlyClosedErrandService recentlyClosedErrandService;
	private final int windowDays;
	private final boolean returnAllTypes;

	EligibilityService(final ErrandRepository errandRepository, final FinancialAssistanceRepository financialAssistanceRepository,
		final LifecareEbCaseService lifecareEbCaseService, final CitizenService citizenService,
		final RecentlyClosedErrandService recentlyClosedErrandService,
		@Value("${financial-assistance.eligibility.duplicate-window-days:90}") final int windowDays,
		@Value("${financial-assistance.eligibility.return-all-types:false}") final boolean returnAllTypes) {
		this.errandRepository = errandRepository;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.lifecareEbCaseService = lifecareEbCaseService;
		this.citizenService = citizenService;
		this.recentlyClosedErrandService = recentlyClosedErrandService;
		this.windowDays = windowDays;
		this.returnAllTypes = returnAllTypes;
	}

	public EligibilityResponse evaluate(final String municipalityId, final String namespace, final EligibilityRequest request) {
		final var today = LocalDate.now();
		final var currentMonth = YearMonth.from(today);
		final var nextMonth = currentMonth.plusMonths(1);
		final var cutoff = OffsetDateTime.now().minusDays(windowDays);
		final var hasCoApplicant = hasText(request.getCoApplicant());

		// TEST OVERRIDE (financial-assistance.eligibility.return-all-types): short-circuit the routing and offer all three
		// application types so the frontend can exercise new application / renewal / supplementary application regardless of
		// any
		// existing case. Turn off to restore the real common-entry point decision flow.
		if (returnAllTypes) {
			return allTypesResponse(hasCoApplicant, currentMonth);
		}

		// Protected identity — hard safety gate (checked in population register/citizen and Lifecare). A protected applicant or
		// co-applicant must not be routed into self-service: offer no application (empty suggestions) and let the frontend
		// hand off to a caseworker. The protected status is kept internal — it never leaves this service. Best-effort per
		// source, so an upstream outage degrades to normal routing rather than blocking every applicant.
		if (hasProtectedIdentity(municipalityId, request.getApplicant())
			|| (hasCoApplicant && hasProtectedIdentity(municipalityId, request.getCoApplicant()))) {
			return protectedIdentityResponse(hasCoApplicant);
		}

		final var response = EligibilityResponse.create()
			.withWindowDays(windowDays)
			.withHasCoApplicant(hasCoApplicant);

		// Lifecare (best-effort).
		var lifecareChecked = true;
		LifecareEbCaseSummary applicantLc;
		LifecareEbCaseSummary coApplicantLc = null;
		try {
			applicantLc = lifecareEbCaseService.summarize(personalNumber(municipalityId, request.getApplicant()), today);
			if (hasCoApplicant) {
				coApplicantLc = lifecareEbCaseService.summarize(personalNumber(municipalityId, request.getCoApplicant()), today);
			}
		} catch (final ThrowableProblem e) {
			lifecareChecked = false;
			applicantLc = LifecareEbCaseSummary.none();
			coApplicantLc = hasCoApplicant ? LifecareEbCaseSummary.none() : null;
		}

		response.setLifecareChecked(lifecareChecked);
		response.setHasPreviousCalculation(applicantLc.hasCalculation());
		ofNullable(applicantLc.latestDecisionPeriod()).ifPresent(period -> {
			response.setLatestDecisionPeriodMonth(period.getMonthValue());
			response.setLatestDecisionPeriodYear(period.getYear());
		});

		// Caremanagement (Druken) EB errands for the applicant (+ co-applicant), scoped to this namespace/municipality.
		final var cmRecords = loadCmRecords(municipalityId, namespace, request);
		final var existsInCm = cmRecords.stream().anyMatch(record -> personIn(record.fa(), request.getApplicant()));
		response.setExistsInCm(existsInCm);
		response.setExistsInLc(applicantLc.hasFootprint());

		// 1) Finns i CM? + LC — applicant must exist; when applying together the co-applicant must too.
		final var applicantExists = existsInCm || applicantLc.hasFootprint();
		final var coApplicantExists = !hasCoApplicant
			|| cmRecords.stream().anyMatch(record -> personIn(record.fa(), request.getCoApplicant()))
			|| (coApplicantLc != null && coApplicantLc.hasFootprint());
		if (!(applicantExists && coApplicantExists)) {
			return newApplication(response, REASON_NO_EXISTING_CASE,
				"No existing errand found" + (lifecareChecked ? "" : " (Lifecare could not be reached)") + ". Suggesting a new application.");
		}

		// 2) Samma marital status? — constellation (alone vs partner, and which partner) vs the most recent existing case.
		// A different co-applicant (a new person, same household size) is a new constellation too → new application.
		final var previousHadCoApplicant = previousHadCoApplicant(cmRecords, applicantLc);
		final var maritalStatusMatches = hasCoApplicant == previousHadCoApplicant && sameCoApplicantIfKnown(cmRecords, request);
		response.setMaritalStatusMatches(maritalStatusMatches);
		if (!maritalStatusMatches) {
			return newApplication(response, REASON_MARITAL_STATUS_CHANGED,
				"The marital status differs from the previous application. Suggesting a new application.");
		}

		// 2.5) Recently closed — a prior EB errand for either party was closed within the recently-closed window. Recommend
		// a renewal and surface the closed errand so a caseworker can reopen it (in Lifecare) and release it for processing.
		final var recentlyClosed = recentlyClosedErrandService.findRecentlyClosed(municipalityId, namespace, parties(request));
		if (recentlyClosed.isPresent()) {
			response.setReopenableErrandId(recentlyClosed.get().errandId());
			response.setClosedAt(recentlyClosed.get().closedAt());
			response.setSuggestions(List.of(suggestion(SLUG_RENEWAL, currentMonth, true)));
			response.setReasonCode(REASON_RECENTLY_CLOSED);
			response.setMessage("A recently closed errand was found. Suggesting a renewal; a caseworker reopens the previous "
				+ "errand and releases it for processing.");
			return response;
		}

		// 3) Per-month — application/decision already present for this/next month?
		final var existsThisMonth = applicationExists(cmRecords, applicantLc, currentMonth, cutoff);
		final var existsNextMonth = applicationExists(cmRecords, applicantLc, nextMonth, cutoff);
		final var currentMonthDecided = applicantLc.decisionMonths().contains(currentMonth);

		response.setApplicationExistsThisMonth(existsThisMonth);
		response.setApplicationExistsNextMonth(existsNextMonth);
		response.setCurrentMonthDecided(currentMonthDecided);

		final var thisMonth = monthSuggestion(currentMonth, existsThisMonth, !currentMonthDecided);
		final var nextMonthSuggestion = monthSuggestion(nextMonth, existsNextMonth, currentMonthDecided);

		// Recommended = the current month while it's still open; next month once the current month is decided.
		response.setSuggestions(currentMonthDecided ? List.of(nextMonthSuggestion, thisMonth) : List.of(thisMonth, nextMonthSuggestion));
		response.setReasonCode(REASON_EXISTING_CASE);
		response.setMessage(currentMonthDecided
			? "A decision exists for the current month. Suggesting a renewal for next month or a supplementary application."
			: "Existing errand without a decision for the current month. Suggesting a renewal for the current month.");
		return response;
	}

	/**
	 * TEST OVERRIDE response: all three application types, with the recommended/new one first. Carries no real CM/Lifecare
	 * facts — it deliberately bypasses every gate so the frontend can open any of the three forms.
	 */
	private static EligibilityResponse allTypesResponse(final boolean hasCoApplicant, final YearMonth currentMonth) {
		return EligibilityResponse.create()
			.withHasCoApplicant(hasCoApplicant)
			.withReasonCode(REASON_ALL_TYPES_TEST)
			.withMessage("Test mode: all application types are returned (the common entry point routing is bypassed).")
			.withSuggestions(List.of(
				suggestion(SLUG_NEW, null, true),
				suggestion(SLUG_RENEWAL, currentMonth, false),
				suggestion(SLUG_SUPPLEMENTARY, currentMonth, false)));
	}

	/**
	 * Protected identity for one person — protected in population register (citizen) <em>or</em> in Lifecare FC. Each
	 * source is
	 * best-effort: a transport/upstream failure is treated as "not protected" so an outage degrades to normal routing
	 * rather than blocking the applicant.
	 */
	private boolean hasProtectedIdentity(final String municipalityId, final String partyId) {
		return citizenProtected(municipalityId, partyId) || lifecareProtected(municipalityId, partyId);
	}

	private boolean citizenProtected(final String municipalityId, final String partyId) {
		try {
			return citizenService.hasProtectedIdentity(municipalityId, partyId);
		} catch (final ThrowableProblem e) {
			return false;
		}
	}

	private boolean lifecareProtected(final String municipalityId, final String partyId) {
		try {
			return citizenService.getPersonalNumber(municipalityId, partyId)
				.map(lifecareEbCaseService::hasProtectedIdentity)
				.orElse(false);
		} catch (final ThrowableProblem e) {
			return false;
		}
	}

	/**
	 * Skyddad-identitet response: an empty suggestion list and nothing else. Deliberately carries no reasonCode, message
	 * or flag — the protected status must not leak across the API edge — so the response is just "nothing can be
	 * recommended"; the frontend turns that into a "contact a caseworker" message.
	 */
	private static EligibilityResponse protectedIdentityResponse(final boolean hasCoApplicant) {
		return EligibilityResponse.create()
			.withHasCoApplicant(hasCoApplicant)
			.withSuggestions(List.of());
	}

	private static EligibilityResponse newApplication(final EligibilityResponse response, final String reasonCode, final String message) {
		response.setSuggestions(List.of(suggestion(SLUG_NEW, null, true)));
		response.setReasonCode(reasonCode);
		response.setMessage(message);
		return response;
	}

	/** A month's suggestion: supplementary application when an application already exists for it, otherwise renewal. */
	private static ApplicationSuggestion monthSuggestion(final YearMonth month, final boolean exists, final boolean recommended) {
		return suggestion(exists ? SLUG_SUPPLEMENTARY : SLUG_RENEWAL, month, recommended);
	}

	/**
	 * Is there an application for {@code month} — a CM errand for that period within the window, or a Lifecare decision?
	 */
	private static boolean applicationExists(final List<CmRecord> cmRecords, final LifecareEbCaseSummary applicantLc,
		final YearMonth month, final OffsetDateTime cutoff) {
		final var inCm = cmRecords.stream().anyMatch(record -> periodEquals(record.fa(), month) && createdWithin(record.errand(), cutoff));
		return inCm || applicantLc.decisionMonths().contains(month);
	}

	/**
	 * The constellation of the most recent existing case — a CM application with a co-applicant, else the latest Lifecare
	 * decision.
	 */
	private static boolean previousHadCoApplicant(final List<CmRecord> cmRecords, final LifecareEbCaseSummary applicantLc) {
		return cmRecords.stream().findFirst()
			.map(record -> hasCoApplicantPerson(record.fa()))
			.orElseGet(applicantLc::hasCoApplicant);
	}

	/**
	 * When applying together, does the requested co-applicant match the most recent existing case's co-applicant? A
	 * different partner is a new constellation → new application. Returns true when applying alone, or when the previous
	 * co-applicant's identity is unknown (inferred from Lifecare, no CM person to compare), so we never force a new
	 * application on missing data.
	 */
	private static boolean sameCoApplicantIfKnown(final List<CmRecord> cmRecords, final EligibilityRequest request) {
		if (!hasText(request.getCoApplicant())) {
			return true;
		}
		return cmRecords.stream().findFirst()
			.flatMap(record -> coApplicantPartyId(record.fa()))
			.map(previous -> previous.equals(request.getCoApplicant()))
			.orElse(true);
	}

	/** The CO_APPLICANT person's partyId on an application, when one is present. */
	private static Optional<String> coApplicantPartyId(final FinancialAssistanceEntity fa) {
		return ofNullable(fa.getPersons()).orElseGet(List::of).stream()
			.filter(person -> ROLE_CO_APPLICANT.equals(person.getRole()))
			.map(FaPerson::getPartyId)
			.filter(StringUtils::hasText)
			.findFirst();
	}

	/** The applicant and (optional) co-applicant partyIds of the request, blanks removed. */
	private static List<String> parties(final EligibilityRequest request) {
		return Stream.of(request.getApplicant(), request.getCoApplicant())
			.filter(StringUtils::hasText)
			.toList();
	}

	/** EB errands (newest first) for either applicant, scoped to the namespace/municipality and the EB type slugs. */
	private List<CmRecord> loadCmRecords(final String municipalityId, final String namespace, final EligibilityRequest request) {
		final var ids = new LinkedHashSet<>(financialAssistanceRepository.findErrandIdsByPartyId(request.getApplicant()));
		if (hasText(request.getCoApplicant())) {
			ids.addAll(financialAssistanceRepository.findErrandIdsByPartyId(request.getCoApplicant()));
		}
		return ids.stream()
			.map(id -> errandRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId))
			.flatMap(Optional::stream)
			.filter(errand -> SLUGS.contains(errand.getTypeSlug()))
			.map(errand -> new CmRecord(errand, financialAssistanceRepository.findByErrandId(errand.getId()).orElse(null)))
			.filter(record -> record.fa() != null)
			.sorted(comparing((CmRecord record) -> record.errand().getCreated(), nullsFirst(naturalOrder())).reversed())
			.toList();
	}

	private static boolean personIn(final FinancialAssistanceEntity fa, final String partyId) {
		return ofNullable(fa.getPersons()).orElseGet(List::of).stream()
			.anyMatch(person -> partyId.equals(person.getPartyId()));
	}

	private static boolean hasCoApplicantPerson(final FinancialAssistanceEntity fa) {
		return ofNullable(fa.getPersons()).orElseGet(List::of).stream()
			.anyMatch(person -> ROLE_CO_APPLICANT.equals(person.getRole()) && hasText(person.getPartyId()));
	}

	/**
	 * Resolve a partyId to the personnummer Lifecare needs; throws (caught upstream → lifecareChecked=false) when unknown.
	 */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}

	private static boolean periodEquals(final FinancialAssistanceEntity fa, final YearMonth month) {
		return ofNullable(fa.getPeriodMonth()).map(m -> m == month.getMonthValue()).orElse(false)
			&& ofNullable(fa.getPeriodYear()).map(y -> y == month.getYear()).orElse(false);
	}

	private static boolean createdWithin(final ErrandEntity errand, final OffsetDateTime cutoff) {
		return errand.getCreated() != null && !errand.getCreated().isBefore(cutoff);
	}

	private static ApplicationSuggestion suggestion(final String slug, final YearMonth period, final boolean recommended) {
		return ApplicationSuggestion.create()
			.withTypeSlug(slug)
			.withApplicationType(applicationTypeFor(slug))
			.withPeriodMonth(period == null ? null : period.getMonthValue())
			.withPeriodYear(period == null ? null : period.getYear())
			.withRecommended(recommended)
			.withLabel(label(slug, period));
	}

	private static String applicationTypeFor(final String slug) {
		return switch (slug) {
			case SLUG_NEW -> APPLICATION_TYPE_NEW;
			case SLUG_RENEWAL -> APPLICATION_TYPE_RENEWAL;
			default -> APPLICATION_TYPE_SUPPLEMENTARY;
		};
	}

	private static String label(final String slug, final YearMonth period) {
		final var base = switch (slug) {
			case SLUG_NEW -> "New application";
			case SLUG_RENEWAL -> "Renewal";
			default -> "Supplementary application";
		};
		return period == null ? base : base + " for " + MONTHS_EN[period.getMonthValue() - 1] + " " + period.getYear();
	}

	/** An EB errand envelope paired with its typed financial-assistance row. */
	private record CmRecord(ErrandEntity errand, FinancialAssistanceEntity fa) {
	}
}
