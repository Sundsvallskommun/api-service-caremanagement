package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.financialaid.integration.FinancialAidIntegration;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekBasis;
import se.sundsvall.dept44.problem.Problem;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The applicant's SSBTEK basis, read live so a caseworker can see what the composite service actually answered.
 * caremanagement only forwards: the applicant is identified by partyId (resolved to a personnummer via the citizen
 * service), the call goes to api-service-financial-aid, and the per-agency answer is returned unmodified.
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

	private final CitizenService citizenService;
	private final FinancialAidIntegration financialAidIntegration;

	FinancialAssistanceSsbtekService(final CitizenService citizenService, final FinancialAidIntegration financialAidIntegration) {
		this.citizenService = citizenService;
		this.financialAidIntegration = financialAidIntegration;
	}

	/**
	 * Fetch the applicant's SSBTEK basis for a period.
	 *
	 * <p>
	 * The window is resolved whole-months so it always lines up with the rule periods: with neither bound given it runs
	 * from the first day of month M−{@value #RULE_PERIOD_LOOKBACK_MONTHS} to the last day of the current month; with one
	 * bound given the other is derived {@value #RULE_PERIOD_LOOKBACK_MONTHS} months from it. Both bounds are echoed back
	 * on the response, so the caller can always state which period the answer covers.
	 * </p>
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  partyId        the applicant's partyId (personId GUID)
	 * @param  from           inclusive start of the period, or {@code null} to derive it
	 * @param  to             inclusive end of the period, or {@code null} to derive it
	 * @return                the per-agency basis plus the resolved period
	 */
	public SsbtekBasis getBasis(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
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

	/** Resolve a partyId to the personnummer SSBTEK needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
