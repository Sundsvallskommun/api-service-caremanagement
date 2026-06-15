package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseSummary;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ApplicationSuggestion;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Optional.ofNullable;
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
 * Application-eligibility (gemensam ingång) routing for ekonomiskt bistånd. Given an applicant and an optional
 * co-applicant, suggests which application to offer — nyansökan / återansökan / tilläggsansökan — following
 * {@code forslag-1-gemensam-ingang-regler}:
 *
 * <ol>
 * <li>If an application was already submitted within the window (our own DB), offer only tilläggsansökan / contact a
 * caseworker.</li>
 * <li>Otherwise read Lifecare (best-effort): no open case for both applicants → nyansökan; an open case with a decision
 * for the current month → återansökan next month or tilläggsansökan this month; an open case without one → återansökan
 * this or next month, or tilläggsansökan.</li>
 * <li>If the requested constellation (alone vs with the given partner) differs from the previous application/decision,
 * flag that a caseworker must handle it.</li>
 * </ol>
 *
 * The decision is advisory — the handläggare stays in the loop — so the result always carries the facts it was built
 * from and a degraded ({@code lifecareChecked=false}) answer when Lifecare is unreachable.
 */
@Service
@Transactional(readOnly = true)
public class EligibilityService {

	static final String REASON_NO_OPEN_CASE = "NO_OPEN_CASE";
	static final String REASON_DECISION_FOR_CURRENT_MONTH = "DECISION_FOR_CURRENT_MONTH";
	static final String REASON_NO_DECISION_FOR_CURRENT_MONTH = "NO_DECISION_FOR_CURRENT_MONTH";
	static final String REASON_RECENT_APPLICATION = "RECENT_APPLICATION";
	static final String REASON_CONSTELLATION_MISMATCH = "CONSTELLATION_MISMATCH";

	private static final String[] MONTHS_SV = {
		"januari", "februari", "mars", "april", "maj", "juni", "juli", "augusti", "september", "oktober", "november", "december"
	};

	private final ErrandRepository errandRepository;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final LifecareEbCaseService lifecareEbCaseService;
	private final int defaultWindowDays;

	EligibilityService(final ErrandRepository errandRepository, final FinancialAssistanceRepository financialAssistanceRepository,
		final LifecareEbCaseService lifecareEbCaseService,
		@Value("${financial-assistance.eligibility.duplicate-window-days:90}") final int defaultWindowDays) {
		this.errandRepository = errandRepository;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.lifecareEbCaseService = lifecareEbCaseService;
		this.defaultWindowDays = defaultWindowDays;
	}

	public EligibilityResponse evaluate(final String municipalityId, final String namespace, final EligibilityRequest request) {
		final var today = LocalDate.now();
		final var currentMonth = YearMonth.from(today);
		final var nextMonth = currentMonth.plusMonths(1);
		final var window = ofNullable(request.getWithinDays()).orElse(defaultWindowDays);
		final var hasCoApplicant = hasText(request.getCoApplicant());

		final var response = EligibilityResponse.create()
			.withWindowDays(window)
			.withHasCoApplicant(hasCoApplicant);

		// 1) Duplicate guard against our own (Druken) errands.
		final var recentErrands = findRecentErrands(municipalityId, namespace, request, OffsetDateTime.now().minusDays(window));
		if (!recentErrands.isEmpty()) {
			final var matches = constellationMatchesDb(recentErrands.get(0).getId(), request);
			response.setHasRecentApplication(true);
			response.setConstellationMatchesPrevious(matches);
			response.setSuggestions(List.of(suggestion(SLUG_SUPPLEMENTARY, currentMonth, true)));
			response.setMessage("En ansökan har redan lämnats in inom " + window
				+ " dagar. Föreslår tilläggsansökan, eller kontakta socialsekreterare.");
			return finishWithConstellation(response, REASON_RECENT_APPLICATION, matches,
				" Sökande skiljer sig från den tidigare ansökan.");
		}

		// 2) Lifecare (best-effort).
		var lifecareChecked = true;
		LifecareEbCaseSummary applicantSummary;
		LifecareEbCaseSummary coApplicantSummary = null;
		try {
			applicantSummary = lifecareEbCaseService.summarize(request.getApplicant(), today);
			if (hasCoApplicant) {
				coApplicantSummary = lifecareEbCaseService.summarize(request.getCoApplicant(), today);
			}
		} catch (final ThrowableProblem e) {
			lifecareChecked = false;
			applicantSummary = LifecareEbCaseSummary.none();
			coApplicantSummary = hasCoApplicant ? LifecareEbCaseSummary.none() : null;
		}

		response.setLifecareChecked(lifecareChecked);
		response.setHasPreviousCalculation(applicantSummary.hasCalculation());
		response.setHasDecisionForCurrentMonth(applicantSummary.hasDecisionForReferenceMonth());
		ofNullable(applicantSummary.latestDecisionPeriod()).ifPresent(period -> {
			response.setLatestDecisionPeriodMonth(period.getMonthValue());
			response.setLatestDecisionPeriodYear(period.getYear());
		});

		// "Öppet ärende för båda sökande" — when applying together, both must have an open case.
		final var openCaseForBoth = applicantSummary.hasOpenCase()
			&& (!hasCoApplicant || (coApplicantSummary != null && coApplicantSummary.hasOpenCase()));
		response.setHasOpenCase(openCaseForBoth);

		// 3) Route.
		if (!openCaseForBoth) {
			response.setSuggestions(List.of(suggestion(SLUG_NEW, null, true)));
			response.setMessage("Inget öppet ärende hittades" + (lifecareChecked ? "" : " (Lifecare kunde inte nås)")
				+ ". Föreslår nyansökan.");
			response.setReasonCode(REASON_NO_OPEN_CASE);
			return response;
		}

		final var matches = constellationMatchesLifecare(applicantSummary, request);
		response.setConstellationMatchesPrevious(matches);

		if (applicantSummary.hasDecisionForReferenceMonth()) {
			response.setSuggestions(List.of(
				suggestion(SLUG_RENEWAL, nextMonth, true),
				suggestion(SLUG_SUPPLEMENTARY, currentMonth, false)));
			response.setMessage("Beslut finns för innevarande månad. Föreslår återansökan för nästa månad "
				+ "eller tilläggsansökan för innevarande månad.");
			return finishWithConstellation(response, REASON_DECISION_FOR_CURRENT_MONTH, matches,
				" Sökande skiljer sig från tidigare beslut – kontakta socialsekreterare.");
		}

		response.setSuggestions(List.of(
			suggestion(SLUG_RENEWAL, currentMonth, true),
			suggestion(SLUG_RENEWAL, nextMonth, false),
			suggestion(SLUG_SUPPLEMENTARY, currentMonth, false)));
		response.setMessage("Öppet ärende utan beslut för innevarande månad. Föreslår återansökan för innevarande "
			+ "eller nästa månad, alternativt tilläggsansökan.");
		return finishWithConstellation(response, REASON_NO_DECISION_FOR_CURRENT_MONTH, matches,
			" Sökande skiljer sig från tidigare beslut – kontakta socialsekreterare.");
	}

	/**
	 * Applies the reason code and, on a constellation mismatch, escalates to a caseworker — overriding the reason code so
	 * the frontend can surface the mismatch as the salient issue while still showing the computed suggestions.
	 */
	private static EligibilityResponse finishWithConstellation(final EligibilityResponse response, final String reasonCode,
		final Boolean matches, final String mismatchNote) {
		if (Boolean.FALSE.equals(matches)) {
			response.setRequiresCaseworker(true);
			response.setReasonCode(REASON_CONSTELLATION_MISMATCH);
			response.setMessage(response.getMessage() + mismatchNote);
		} else {
			response.setReasonCode(reasonCode);
		}
		return response;
	}

	/** Recent financial-assistance errands (created within the window) in this namespace where either applicant appears. */
	private List<ErrandEntity> findRecentErrands(final String municipalityId, final String namespace,
		final EligibilityRequest request, final OffsetDateTime createdAfter) {
		final var ids = new LinkedHashSet<>(financialAssistanceRepository.findRecentErrandIdsByPerson(request.getApplicant(), createdAfter));
		if (hasText(request.getCoApplicant())) {
			ids.addAll(financialAssistanceRepository.findRecentErrandIdsByPerson(request.getCoApplicant(), createdAfter));
		}
		return ids.stream()
			.map(id -> errandRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId))
			.flatMap(Optional::stream)
			.filter(errand -> SLUGS.contains(errand.getTypeSlug()))
			.sorted(comparing(ErrandEntity::getCreated, nullsFirst(naturalOrder())).reversed())
			.toList();
	}

	/** Does the requested constellation match the persons on a previous (DB) application? */
	private Boolean constellationMatchesDb(final String errandId, final EligibilityRequest request) {
		final var previousCoApplicants = financialAssistanceRepository.findByErrandId(errandId)
			.map(entity -> ofNullable(entity.getPersons()).orElseGet(List::of).stream()
				.filter(person -> ROLE_CO_APPLICANT.equals(person.getRole()))
				.map(FaPerson::getPersonalNumber)
				.filter(StringUtils::hasText)
				.toList())
			.orElseGet(List::of);
		return matchesConstellation(previousCoApplicants, request.getCoApplicant());
	}

	/**
	 * Does the requested constellation match the co-applicants on the latest Lifecare decision (or {@code null} if none)?
	 */
	private static Boolean constellationMatchesLifecare(final LifecareEbCaseSummary summary, final EligibilityRequest request) {
		if (summary.latestDecisionPeriod() == null) {
			return null; // nothing to compare against
		}
		return matchesConstellation(List.copyOf(summary.coApplicantPersonIds()), request.getCoApplicant());
	}

	/**
	 * Applying together → the previous co-applicants must include the requested partner; applying alone → there were none.
	 */
	private static Boolean matchesConstellation(final List<String> previousCoApplicants, final String requestedCoApplicant) {
		if (hasText(requestedCoApplicant)) {
			return previousCoApplicants.contains(requestedCoApplicant);
		}
		return previousCoApplicants.isEmpty();
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
			case SLUG_NEW -> "Nyansökan";
			case SLUG_RENEWAL -> "Återansökan";
			default -> "Tilläggsansökan";
		};
		return period == null ? base : base + " för " + MONTHS_SV[period.getMonthValue() - 1] + " " + period.getYear();
	}
}
