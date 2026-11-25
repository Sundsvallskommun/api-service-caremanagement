package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static java.util.Optional.ofNullable;

/**
 * The SSBTEK period checks — the verksamhet's dag- and glappkontroller encoded as the modeler-editable
 * {@code rakel-eb-periodkontroll} DMN deployment. Same division of labour as the other EB decisions
 * ({@link ExpenseRulesService}, {@link ApplicationRulesService}): the table <em>judges</em>, the caller
 * ({@link PeriodRuleFeeder}) selects the incomes the rule applies to and works out the numbers.
 *
 * <ul>
 * <li>{@code Decision_dagersattningDagkontroll} — aktivitetsstöd / utvecklings- / etableringsersättning: verksamhetens
 * gate (AF ekonomiskt beslut, FK:s 450 dagar), then a missing payment, then the day count against the non-red days of
 * the covered month ({@link #dayCheck})</li>
 * </ul>
 *
 * <p>
 * {@code Decision_foraldrapenningKontroll} used to sit alongside it — day count against the period, plus the gap to
 * last month's. Verksamheten retired both on 2026-09-21: „detta ska inte göras, denna inkomst kommer inte vara med
 * på rålistan”. The table is gone from the published DMN and the check with it.
 * </p>
 *
 * <p>
 * The tables output {@code varning} + {@code regel} and no warning code, so the caller supplies the warning type and
 * the table supplies the text. Best-effort throughout: an unavailable decision or an undeployed key yields no warning
 * rather than an error, so the daily prepare is never blocked.
 * </p>
 */
@Service
public class PeriodRulesService {

	static final String DECISION_KEY_DAY_CHECK = "Decision_dagersattningDagkontroll";

	private static final String OUTPUT_VARNING = "varning";
	private static final String OUTPUT_REGEL = "regel";

	private static final Logger LOG = LoggerFactory.getLogger(PeriodRulesService.class);

	private final ProcessService processService;

	PeriodRulesService(final ProcessService processService) {
		this.processService = processService;
	}

	/** A period-check verdict — whether to flag, and the verksamhet's own text for why. */
	public record PeriodVerdict(boolean warning, String rule) {

		/** The neutral verdict — nothing to flag. */
		public static PeriodVerdict none() {
			return new PeriodVerdict(false, null);
		}
	}

	/**
	 * What the dagersättning day check is asked, in the table's own order (verksamhetens svar 2026-09-23 §2). Every boxed
	 * field is nullable, and {@code null} is meaningful: an unread gate ({@code economicDecision} or
	 * {@code allDaysConsumed}) makes the table stop without a warning.
	 *
	 * @param economicDecision      AF reports an ekonomiskt beslut for the control month ({@code afEkonomisktBeslut})
	 * @param allDaysConsumed       FK reports all 450 days used up ({@code allaDagarForbrukade})
	 * @param paymentFound          the control month carries a matching payment ({@code utbetalningFinns})
	 * @param periodReadable        the payment's period reads as one whole month ({@code periodLasbar}); {@code null}
	 *                              without a payment
	 * @param days                  the days the payment is for ({@code uttagnaDagar}), possibly a half day
	 * @param nonRedDays            the non-red days of the covered month ({@code ickeRodaDagar})
	 * @param nonRedDaysAlternative the tolerance input ({@code ickeRodaDagarAlternativ}); equal to {@code nonRedDays}
	 *                              since verksamheten decided midsommarafton, see {@link NonRedDayCalendar}
	 */
	public record DayCheck(Boolean economicDecision, Boolean allDaysConsumed, boolean paymentFound, Boolean periodReadable,
		BigDecimal days, Integer nonRedDays, Integer nonRedDaysAlternative) {}

	/**
	 * Judge the dagersättning day check for one payment — or, with {@code paymentFound = false}, for its absence.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  check          the gate and the numbers, see {@link DayCheck}
	 * @return                the verdict, best-effort
	 */
	public PeriodVerdict dayCheck(final String municipalityId, final DayCheck check) {
		final var variables = new HashMap<String, Object>();
		variables.put("afEkonomisktBeslut", check.economicDecision());
		variables.put("allaDagarForbrukade", check.allDaysConsumed());
		variables.put("utbetalningFinns", check.paymentFound());
		variables.put("periodLasbar", check.periodReadable());
		variables.put("uttagnaDagar", check.days());
		variables.put("ickeRodaDagar", check.nonRedDays());
		variables.put("ickeRodaDagarAlternativ", check.nonRedDaysAlternative());
		return evaluate(municipalityId, DECISION_KEY_DAY_CHECK, variables);
	}

	private PeriodVerdict evaluate(final String municipalityId, final String decisionKey, final Map<String, Object> variables) {
		try {
			final var rows = processService.evaluateDecision(municipalityId, decisionKey, variables);
			return firstRow(rows).map(PeriodRulesService::toVerdict).orElseGet(PeriodVerdict::none);
		} catch (final RuntimeException e) {
			LOG.warn("Period rules ({}) unavailable — skipping the period warning", decisionKey, e);
			return PeriodVerdict.none();
		}
	}

	private static Optional<Map<String, Object>> firstRow(final List<Map<String, Object>> rows) {
		return ofNullable(rows).orElseGet(List::of).stream().findFirst();
	}

	private static PeriodVerdict toVerdict(final Map<String, Object> row) {
		return new PeriodVerdict(Boolean.TRUE.equals(row.get(OUTPUT_VARNING)),
			ofNullable(row.get(OUTPUT_REGEL)).map(Object::toString).orElse(null));
	}
}
