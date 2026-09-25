package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.financialaid.integration.FinancialAidIntegration;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekBasis;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * An errand's household member's SSBTEK basis, read live so a caseworker can see what the composite service actually
 * answered (GUI-01, manual SSBTEK check). caremanagement only forwards: the person is the errand's applicant,
 * co-applicant or one of the household children on the application — resolved from the errand the way the beredning
 * resolves them, never taken from the caller (a child's partyId is only accepted when the errand names that child) —
 * their personnummer comes from the citizen service, the call goes to api-service-financial-aid, and the per-agency
 * answer is returned unmodified. Scoping the read to an errand is what puts it in the errand's access log: the path
 * carries the errand id, so {@code ErrandEventInterceptor} records who read whose SSBTEK data and when.
 *
 * <p>
 * Nothing is stored. The basis is income data for a named person, so it is held only for the length of the request —
 * and, like everywhere else in the SSBTEK pipeline, neither the personnummer nor any part of the answer is logged.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class FinancialAssistanceSsbtekService {

	/**
	 * How many months the default window spans backwards. The three SSBTEK rule periods are the application month M
	 * (ansökningsperiod), M−1 (kontrollperiod) and M−2 (jämförelseperiod), so the default covers M−2 through M — the same
	 * window the {@code fetch-financial-aid-basis} BPMN task asks for, which keeps this read comparable with what the
	 * process saw. See docs/ssbtek-regelverk.txt in the sprint workspace.
	 */
	private static final int RULE_PERIOD_LOOKBACK_MONTHS = 2;

	private static final String ROLE_CHILD = "CHILD";

	private final ErrandService errandService;
	private final StakeholderService stakeholderService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final CitizenService citizenService;
	private final FinancialAidIntegration financialAidIntegration;

	FinancialAssistanceSsbtekService(final ErrandService errandService, final StakeholderService stakeholderService,
		final FinancialAssistanceRepository financialAssistanceRepository, final CitizenService citizenService, final FinancialAidIntegration financialAidIntegration) {
		this.errandService = errandService;
		this.stakeholderService = stakeholderService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.citizenService = citizenService;
		this.financialAidIntegration = financialAidIntegration;
	}

	/**
	 * Fetch the SSBTEK basis of an errand's applicant, co-applicant or household child for a period. Scoped: {@code 404}
	 * when the errand is missing in this namespace/municipality, when it has no member in the asked-for role, when the
	 * asked-for child is not one of the errand's household children, or when the citizen is unknown. A child is named by
	 * {@code childPartyId}, which is required with role {@code CHILD} and rejected ({@code 400}) with any other role.
	 *
	 * <p>
	 * The window is resolved whole-months so it always lines up with the rule periods: with neither bound given it runs
	 * from the first day of month M−{@value #RULE_PERIOD_LOOKBACK_MONTHS} to the last day of the current month; with one
	 * bound given the other is derived {@value #RULE_PERIOD_LOOKBACK_MONTHS} months from it. Both bounds are echoed back
	 * on the response, so the caller can always state which period the answer covers.
	 * </p>
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  namespace      the errand's namespace
	 * @param  errandId       the errand whose household member is read
	 * @param  role           {@code APPLICANT}, {@code CO_APPLICANT} or {@code CHILD}
	 * @param  childPartyId   the household child to read when {@code role} is {@code CHILD}, otherwise {@code null}
	 * @param  from           inclusive start of the period, or {@code null} to derive it
	 * @param  to             inclusive end of the period, or {@code null} to derive it
	 * @return                the per-agency basis plus the resolved period
	 */
	public SsbtekBasis getBasis(final String municipalityId, final String namespace, final String errandId, final String role, final String childPartyId,
		final LocalDate from, final LocalDate to) {
		validateChildSelection(role, childPartyId);
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var partyId = resolvePartyId(municipalityId, namespace, errandId, role, childPartyId);
		final var applicant = personalNumber(municipalityId, partyId);
		final var fromDate = resolveFrom(from, to);
		final var toDate = resolveTo(from, to);

		if (fromDate.isAfter(toDate)) {
			throw Problem.valueOf(BAD_REQUEST, "'from' must not be after 'to'");
		}

		return SsbtekBasis.create()
			.withFrom(fromDate)
			.withTo(toDate)
			.withAgencies(basisFor(municipalityId, applicant, fromDate, toDate));
	}

	/** The explicit start, else the first day of the month {@value #RULE_PERIOD_LOOKBACK_MONTHS} back from the end. */
	private static LocalDate resolveFrom(final LocalDate from, final LocalDate to) {
		if (from != null) {
			return from;
		}
		final var lastMonth = ofNullable(to).map(YearMonth::from).orElseGet(YearMonth::now);
		return lastMonth.minusMonths(RULE_PERIOD_LOOKBACK_MONTHS).atDay(1);
	}

	/** The explicit end, else the last day of the month {@value #RULE_PERIOD_LOOKBACK_MONTHS} on from the start. */
	private static LocalDate resolveTo(final LocalDate from, final LocalDate to) {
		if (to != null) {
			return to;
		}
		final var firstMonth = ofNullable(from).map(YearMonth::from).orElseGet(() -> YearMonth.now().minusMonths(RULE_PERIOD_LOOKBACK_MONTHS));
		return firstMonth.plusMonths(RULE_PERIOD_LOOKBACK_MONTHS).atEndOfMonth();
	}

	/** Never returns {@code null} — an agency-less answer is rendered as an empty map so the frontend has one shape. */
	private Map<String, Map<String, Object>> basisFor(final String municipalityId, final String applicant, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(financialAidIntegration.getFinancialAidBasis(municipalityId, applicant, fromDate.format(ISO_LOCAL_DATE), toDate.format(ISO_LOCAL_DATE)))
			.orElseGet(Map::of);
	}

	/** A child is named by partyId and only a child is: the pairing is checked before anything is read. */
	private static void validateChildSelection(final String role, final String childPartyId) {
		final var isChild = ROLE_CHILD.equals(role);
		if (isChild && childPartyId == null) {
			throw Problem.valueOf(BAD_REQUEST, "'childPartyId' is required when person is CHILD");
		}
		if (!isChild && childPartyId != null) {
			throw Problem.valueOf(BAD_REQUEST, "'childPartyId' is only allowed when person is CHILD");
		}
	}

	private String resolvePartyId(final String municipalityId, final String namespace, final String errandId, final String role, final String childPartyId) {
		if (ROLE_CHILD.equals(role)) {
			return householdChildPartyId(errandId, childPartyId);
		}
		return householdPartyId(municipalityId, namespace, errandId, role);
	}

	/** The child's partyId when the errand's application names that child, or 404 — never a caller-chosen person. */
	private String householdChildPartyId(final String errandId, final String childPartyId) {
		return financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getChildren)
			.orElseGet(List::of)
			.stream()
			.map(FaChild::getPartyId)
			.filter(childPartyId::equals)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Errand %s has no household child with partyId %s".formatted(errandId, childPartyId)));
	}

	/** The partyId of the errand's member in {@code role}, or 404 when the household has none. */
	private String householdPartyId(final String municipalityId, final String namespace, final String errandId, final String role) {
		final var persons = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getPersons)
			.orElseGet(List::of);
		return HouseholdPartyService.resolvePartyId(stakeholderService.readAll(municipalityId, namespace, errandId), persons, role)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Errand %s has no household member with role %s".formatted(errandId, role)));
	}

	/** Resolve a partyId to the personnummer SSBTEK needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
