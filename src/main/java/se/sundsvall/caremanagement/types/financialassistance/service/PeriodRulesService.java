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
 * <li>{@code Decision_dagersattningDagkontroll} — aktivitetsstöd / utvecklings- / etableringsersättning: is the day
 * count readable, and does it match the month? ({@link #dayCheck})</li>
 * <li>{@code Decision_foraldrapenningKontroll} — föräldrapenning: does the day count match the period, and is there a
 * gap since last month's period? ({@link #parentalBenefitCheck})</li>
 * </ul>
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
	static final String DECISION_KEY_PARENTAL_BENEFIT = "Decision_foraldrapenningKontroll";

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
	 * Judge one dagersättning payment's day count.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  periodReadable whether the payment's period could be read as a single whole month
	 * @param  days           the number of days drawn ({@code uttagnaDagar}), {@code null} when SSBTEK did not say
	 * @param  nonRedDays     the number of non-red days in that month ({@code ickeRodaDagar}); see
	 *                        {@link PeriodRuleFeeder} for why the caller never reaches this decision without one
	 * @return                the verdict, best-effort
	 */
	public PeriodVerdict dayCheck(final String municipalityId, final boolean periodReadable, final BigDecimal days, final Integer nonRedDays) {
		final var variables = new HashMap<String, Object>();
		variables.put("periodLasbar", periodReadable);
		variables.put("uttagnaDagar", days);
		variables.put("ickeRodaDagar", nonRedDays);
		return evaluate(municipalityId, DECISION_KEY_DAY_CHECK, variables);
	}

	/**
	 * Judge one föräldrapenning payment's day count and its gap to the previous month's period.
	 *
	 * @param  municipalityId      the municipality the errand belongs to
	 * @param  periodReadable      whether the payment's period could be read at all ({@code periodLasbar})
	 * @param  days                the number of days drawn ({@code uttagnaDagar}), {@code null} when SSBTEK did not say
	 * @param  daysInPeriod        the number of days the period covers, both ends included ({@code dagarIPerioden})
	 * @param  previousMonthExists whether föräldrapenning was paid in the comparison period too
	 * @param  gapDays             days between the comparison period's last day and this period's first
	 *                             ({@code glappDagar}); {@code null} when there is no previous period to measure from
	 * @return                     the verdict, best-effort
	 */
	public PeriodVerdict parentalBenefitCheck(final String municipalityId, final boolean periodReadable, final BigDecimal days,
		final Integer daysInPeriod, final boolean previousMonthExists, final Integer gapDays) {

		final var variables = new HashMap<String, Object>();
		variables.put("periodLasbar", periodReadable);
		variables.put("uttagnaDagar", days);
		variables.put("dagarIPerioden", daysInPeriod);
		variables.put("foregaendeManadFinns", previousMonthExists);
		variables.put("glappDagar", gapDays);
		return evaluate(municipalityId, DECISION_KEY_PARENTAL_BENEFIT, variables);
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
