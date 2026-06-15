package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionPersonDTO;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * Answers EB-routing questions about a person from Lifecare FC. Wraps {@link LifecareFcIntegration}, reading
 * aktualiseringar, beslut and normberäkningar over a lookback window ending at the reference date, and reduces them to
 * a
 * domain {@link LifecareEbCaseSummary}. FC's date strings (from/to periods) and generated DTOs never leave this module.
 *
 * <p>
 * Calls propagate the integration's {@code BAD_GATEWAY} problem on failure — the caller decides whether to treat the
 * lookup as best-effort.
 * </p>
 */
@Service
public class LifecareEbCaseService {

	/** Guard against a malformed decision period (e.g. from 2000 to 2030) producing an unbounded month set. */
	private static final int MAX_MONTHS_PER_DECISION = 36;

	private final LifecareFcIntegration lifecareFcIntegration;
	private final int lookbackMonths;

	LifecareEbCaseService(final LifecareFcIntegration lifecareFcIntegration,
		@Value("${integration.lifecare-fc.eb-lookback-months:13}") final int lookbackMonths) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.lookbackMonths = lookbackMonths;
	}

	/**
	 * Summarises the person's EB footprint in FC over the lookback window ending at {@code referenceDate}.
	 *
	 * @param  personId      the person's personnummer
	 * @param  referenceDate the date the routing is evaluated against (bounds the lookback window)
	 * @return               the distilled summary; never {@code null}
	 */
	public LifecareEbCaseSummary summarize(final String personId, final LocalDate referenceDate) {
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		final var actualisations = ofNullable(lifecareFcIntegration.getActualisations(personId, start, end, null, null, false))
			.map(ApiPaginationCompositePersonBasedAktualiseringDTO::getResult)
			.orElseGet(List::of);

		final var decisions = ofNullable(lifecareFcIntegration.getDecisions(personId, start, end, null, null, false))
			.map(ApiPaginationCompositePersonBasedDecisionDTO::getResult)
			.orElseGet(List::of);

		final var calculations = ofNullable(lifecareFcIntegration.getCalculations(personId, start, end, null, null, false))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of);

		final var hasFootprint = !actualisations.isEmpty() || !decisions.isEmpty() || !calculations.isEmpty();

		final var decisionMonths = new TreeSet<YearMonth>();
		decisions.forEach(decision -> decisionMonths.addAll(monthsCovered(decision)));

		final var latestDecision = latestDecision(decisions);

		return new LifecareEbCaseSummary(
			hasFootprint,
			Set.copyOf(decisionMonths),
			latestDecision.map(LifecareEbCaseService::periodOf).orElse(null),
			!calculations.isEmpty(),
			latestDecision.map(LifecareEbCaseService::hasCoApplicant).orElse(false));
	}

	/** The decision with the most recent period (to/from), used to read the current household constellation. */
	private static Optional<PersonBasedDecisionDTO> latestDecision(final List<PersonBasedDecisionDTO> decisions) {
		return decisions.stream()
			.filter(decision -> periodOf(decision) != null)
			.max(comparing(LifecareEbCaseService::periodOf));
	}

	/** Whether a decision included a co-applicant — a flagged participant or the scalar {@code coApplicant} field. */
	private static boolean hasCoApplicant(final PersonBasedDecisionDTO decision) {
		final var flagged = ofNullable(decision.getDecisionPersonDTOs()).orElseGet(List::of).stream()
			.filter(person -> Boolean.TRUE.equals(person.getIsCoApplicant()))
			.map(PersonBasedDecisionPersonDTO::getPersonId)
			.anyMatch(StringUtils::hasText);
		return flagged || hasText(decision.getCoApplicant());
	}

	/** The year-months a decision's from/to period covers (bounded; a single side covers just that month). */
	private static List<YearMonth> monthsCovered(final PersonBasedDecisionDTO decision) {
		final var from = toYearMonth(decision.getFromDate());
		final var to = toYearMonth(decision.getToDate());
		if (from.isPresent() && to.isPresent()) {
			final var months = new ArrayList<YearMonth>();
			var cursor = from.get();
			final var last = to.get();
			while (!cursor.isAfter(last) && months.size() < MAX_MONTHS_PER_DECISION) {
				months.add(cursor);
				cursor = cursor.plusMonths(1);
			}
			return months;
		}
		return from.or(() -> to).map(List::of).orElseGet(List::of);
	}

	/** The representative period of a decision — its {@code toDate} month, falling back to {@code fromDate}. */
	private static YearMonth periodOf(final PersonBasedDecisionDTO decision) {
		return toYearMonth(decision.getToDate()).or(() -> toYearMonth(decision.getFromDate())).orElse(null);
	}

	/** Lenient year-month extraction from FC's date strings ("yyyy-MM-dd", "yyyy-MM", or an ISO datetime). */
	static Optional<YearMonth> toYearMonth(final String value) {
		if (!hasText(value)) {
			return Optional.empty();
		}
		final var datePart = value.trim().split("[T ]", 2)[0];
		final var parts = datePart.split("-");
		if (parts.length < 2) {
			return Optional.empty();
		}
		try {
			return Optional.of(YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
		} catch (final NumberFormatException | DateTimeException e) {
			return Optional.empty();
		}
	}
}
