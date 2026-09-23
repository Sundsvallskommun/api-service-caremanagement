package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DayCheckBasis;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EconomicDecisionPeriod;

import static java.util.Optional.ofNullable;

/**
 * Turns the SSBTEK incomes into the period-check warnings, delegating every judgement to the
 * {@link PeriodRulesService} DMN table ({@code rakel-eb-periodkontroll}). The input-gathering half of the division of
 * labour: it picks out the payments the rule applies to, resolves verksamhetens gate, decides whether each payment's
 * period could be read and counts the non-red days of the month it covers — the table decides whether any of that is
 * worth a warning.
 *
 * <p>
 * <strong>The gate</strong> (verksamhetens svar 2026-09-23 §2): no check and no warning unless Arbetsförmedlingen
 * reports an ekonomiskt beslut for the control month and Försäkringskassan does not report all 450 days of the jobb-
 * och utvecklingsgaranti as used up. Neither fact is an income, so they arrive beside the classified incomes, in the
 * request's {@link DayCheckBasis}. <strong>Absent means unread, and an unread gate stops the check</strong> — the
 * engine does not send the basis yet, so until it does the day check is wired but silent.
 * </p>
 *
 * <p>
 * <strong>Periods.</strong> The payment belongs to the control month by its payment date (the engine's period
 * attribution, unchanged); the day count is compared against the non-red days of the month the payment <em>covers</em>
 * ({@code periodFran}–{@code periodTill}, "gör om till hela månaden"), counted by {@link NonRedDayCalendar}.
 * </p>
 *
 * <p>
 * <strong>Selection</strong> follows the regelverk literally: benefit "Dagersättning", plus a sub-benefit and an
 * amount type from the rule's own lists. Both of those only arrive when Försäkringskassan's payment carries exactly
 * one {@code utbetalningsdetalj} — the deliberately conservative reading in the operaton extractor — so a payment
 * split across several detail rows matches no list. Such a payment is not checked, and because it may well be the
 * aktivitetsstöd, its presence also suppresses the "saknas utbetalning" warning: guessing either way would put a
 * fabricated claim in front of a handläggare.
 * </p>
 */
@Service
public class PeriodRuleFeeder {

	/** Aktivitetsstöd / utvecklings- / etableringsersättning — the regelverk's sub-benefit list. */
	private static final Set<String> DAY_BENEFIT_SUB_BENEFITS = Set.of(
		"arbetsmarknadspolitiskt program", "arbetsmarknadspolitiskt pgm", "dagersättning");

	/** …and its amount-type list. */
	private static final Set<String> DAY_BENEFIT_AMOUNT_TYPES = Set.of(
		"aktivitetsstöd", "etableringsersättning", "bostadsersättning", "etableringstillägg", "utvecklingsersättning");

	private static final String BENEFIT_DAY_ALLOWANCE = "dagersättning";

	/** The jobb- och utvecklingsgaranti's day limit. */
	static final int MAX_GUARANTEE_DAYS = 450;

	private static final String RULE_KEY = "DAGERSATTNING";
	private static final String SOURCE_KEY_MISSING_PAYMENT = RULE_KEY + ":SAKNAS:%s";

	private final PeriodRulesService periodRulesService;

	PeriodRuleFeeder(final PeriodRulesService periodRulesService) {
		this.periodRulesService = periodRulesService;
	}

	/** The resolved gate: {@code null} in either half means "not read". */
	private record Gate(Boolean economicDecision, Boolean allDaysConsumed) {}

	/**
	 * The period-check warnings for this month's SSBTEK incomes.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  controlMonth   the kontrollmånad — the month before the application month
	 * @param  classified     the classified incomes as the engine sent them, both periods
	 * @param  basis          the AF/FK gate facts as the engine sent them; {@code null} when it sent none
	 * @return                the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> periodWarnings(final String municipalityId, final YearMonth controlMonth,
		final List<ClassifiedIncome> classified, final DayCheckBasis basis) {

		final var control = periodIncomes(ofNullable(classified).orElseGet(List::of), false);
		final var gate = gate(controlMonth, basis);
		final var payments = control.stream().filter(PeriodRuleFeeder::isDayBenefit).toList();

		if (payments.isEmpty()) {
			if (control.stream().anyMatch(PeriodRuleFeeder::isUnclassifiableDayAllowance)) {
				return List.of();
			}
			final var verdict = periodRulesService.dayCheck(municipalityId,
				new PeriodRulesService.DayCheck(gate.economicDecision(), gate.allDaysConsumed(), false, null, null, null, null));
			return warning(SOURCE_KEY_MISSING_PAYMENT.formatted(controlMonth), verdict).stream().toList();
		}

		final var warnings = new ArrayList<WarningService.WarningInput>();
		payments.forEach(payment -> warning(sourceKey(payment),
			periodRulesService.dayCheck(municipalityId, paymentCheck(gate, payment))).ifPresent(warnings::add));
		return List.copyOf(warnings);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Decision_dagersattningDagkontroll
	// ------------------------------------------------------------------------------------------------------------

	private static PeriodRulesService.DayCheck paymentCheck(final Gate gate, final SsbtekIncome payment) {
		if (!wholeMonth(payment)) {
			return new PeriodRulesService.DayCheck(gate.economicDecision(), gate.allDaysConsumed(), true, false, payment.days(), null, null);
		}
		final var nonRedDays = NonRedDayCalendar.nonRedDays(YearMonth.from(payment.periodFrom()));
		return new PeriodRulesService.DayCheck(gate.economicDecision(), gate.allDaysConsumed(), true, true, payment.days(),
			nonRedDays.count(), nonRedDays.alternativeCount());
	}

	/**
	 * The gate, resolved from what the engine sent. An AF decision counts when its period overlaps the control month —
	 * the month whose SSBTEK answer is checked for the payment. All days count as used up when FK says so or when the
	 * consumed days reach {@value #MAX_GUARANTEE_DAYS}.
	 */
	private static Gate gate(final YearMonth controlMonth, final DayCheckBasis basis) {
		final var facts = ofNullable(basis);
		final var economicDecision = facts.map(DayCheckBasis::getEconomicDecisionPeriods)
			.map(periods -> periods.stream().filter(Objects::nonNull).anyMatch(period -> overlaps(period, controlMonth)))
			.orElse(null);
		final var allDaysConsumed = facts.map(PeriodRuleFeeder::allDaysConsumed).orElse(null);
		return new Gate(economicDecision, allDaysConsumed);
	}

	private static Boolean allDaysConsumed(final DayCheckBasis basis) {
		final var byCount = ofNullable(basis.getConsumedDays()).map(days -> days >= MAX_GUARANTEE_DAYS);
		if (Boolean.TRUE.equals(basis.getAllDaysConsumed()) || byCount.orElse(false)) {
			return true;
		}
		return ofNullable(basis.getAllDaysConsumed()).or(() -> byCount).orElse(null);
	}

	private static boolean overlaps(final EconomicDecisionPeriod period, final YearMonth month) {
		final var startsInTime = ofNullable(period.fromDate()).map(from -> !from.isAfter(month.atEndOfMonth())).orElse(true);
		final var endsInTime = ofNullable(period.toDate()).map(to -> !to.isBefore(month.atDay(1))).orElse(true);
		return startsInTime && endsInTime;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Selection and arithmetic
	// ------------------------------------------------------------------------------------------------------------

	private static List<SsbtekIncome> periodIncomes(final List<ClassifiedIncome> classified, final boolean fromComparisonPeriod) {
		return classified.stream()
			.filter(income -> income.isFromComparisonPeriod() == fromComparisonPeriod)
			.map(ClassifiedIncome::income)
			.filter(Objects::nonNull)
			.toList();
	}

	private static boolean isDayBenefit(final SsbtekIncome income) {
		return BENEFIT_DAY_ALLOWANCE.equals(normalize(income.benefit()))
			&& DAY_BENEFIT_SUB_BENEFITS.contains(normalize(income.subBenefit()))
			&& DAY_BENEFIT_AMOUNT_TYPES.contains(normalize(income.amountType()));
	}

	/** A Dagersättning payment the extractor could not classify — FK split it over several detail rows. */
	private static boolean isUnclassifiableDayAllowance(final SsbtekIncome income) {
		return BENEFIT_DAY_ALLOWANCE.equals(normalize(income.benefit())) && (income.subBenefit() == null) && (income.amountType() == null);
	}

	private static String normalize(final String value) {
		return ofNullable(value).map(text -> text.trim().toLowerCase(Locale.ROOT)).orElse("");
	}

	/**
	 * Whether the payment's period reads as one whole month. The regelverk wants the period "omgjord till hela
	 * månaden" and calls out a period spanning two months as the case that cannot be read.
	 */
	private static boolean wholeMonth(final SsbtekIncome income) {
		return hasPeriod(income) && YearMonth.from(income.periodFrom()).equals(YearMonth.from(income.periodTo()));
	}

	private static boolean hasPeriod(final SsbtekIncome income) {
		return (income.periodFrom() != null) && (income.periodTo() != null);
	}

	/** One warning per (benefit, period start), so the daily reconcile dedups and auto-closes it. */
	private static String sourceKey(final SsbtekIncome income) {
		return RULE_KEY + ":" + ofNullable(income.periodFrom()).map(LocalDate::toString).orElseGet(
			() -> ofNullable(income.period()).map(LocalDate::toString).orElse("okand-period"));
	}

	private static Optional<WarningService.WarningInput> warning(final String sourceKey, final PeriodRulesService.PeriodVerdict verdict) {
		if (!verdict.warning()) {
			return Optional.empty();
		}
		return Optional.of(new WarningService.WarningInput(WarningService.TYPE_SSBTEK_DAY_CHECK, sourceKey, verdict.rule()));
	}
}
