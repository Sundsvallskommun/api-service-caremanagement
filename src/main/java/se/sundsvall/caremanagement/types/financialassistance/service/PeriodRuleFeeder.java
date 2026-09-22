package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;

/**
 * Turns the SSBTEK incomes into the period-check warnings, delegating every judgement to the
 * {@link PeriodRulesService} DMN tables ({@code rakel-eb-periodkontroll}). The input-gathering half of the division of
 * labour: it picks out the payments each rule applies to, decides whether the period could be read, counts the days
 * the period covers and measures the gap to the previous month — the tables decide whether any of that is worth a
 * warning.
 *
 * <p>
 * <strong>Selection</strong> follows the regelverk literally: benefit "Dagersättning", plus a sub-benefit and an
 * amount type from the rule's own lists. Both of those only arrive when Försäkringskassan's payment carries exactly
 * one {@code utbetalningsdetalj} — the deliberately conservative reading in the operaton extractor — so a payment
 * split across several detail rows matches neither rule and is checked by nobody. That is a known gap, not an
 * oversight: guessing the sub-benefit of a split payment would put a fabricated classification in front of a
 * handläggare.
 * </p>
 *
 * <p>
 * <strong>The dagersättning day check is only partly wired.</strong> Its third question — does the day count match the
 * number of non-red days in the month — needs a public-holiday calendar, and the solution deliberately has none: the
 * hand-derived one was reverted on 2026-09-11 because a count we compute ourselves cannot be falsified against the
 * authority's own arithmetic. So the two branches that need no calendar are evaluated (the period could not be read;
 * the day count is missing) and the comparison itself is not: passing a {@code null} {@code ickeRodaDagar} would make
 * FEEL's {@code uttagnaDagar = null} false and raise "dagarna stämmer inte" on every correct payment. A payment whose
 * period and day count are both readable is therefore left unchecked until the calendar question is answered.
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

	private static final Logger LOG = LoggerFactory.getLogger(PeriodRuleFeeder.class);

	private final PeriodRulesService periodRulesService;

	PeriodRuleFeeder(final PeriodRulesService periodRulesService) {
		this.periodRulesService = periodRulesService;
	}

	/**
	 * The period-check warnings for this month's SSBTEK incomes.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  classified     the classified incomes as the engine sent them, both periods
	 * @return                the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> periodWarnings(final String municipalityId, final List<ClassifiedIncome> classified) {
		final var incomes = ofNullable(classified).orElseGet(List::of);
		final var control = periodIncomes(incomes, false);

		final var warnings = new ArrayList<WarningService.WarningInput>();
		control.stream().filter(PeriodRuleFeeder::isDayBenefit)
			.map(income -> dayBenefitWarning(municipalityId, income))
			.forEach(warning -> warning.ifPresent(warnings::add));
		return List.copyOf(warnings);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Decision_dagersattningDagkontroll
	// ------------------------------------------------------------------------------------------------------------

	private Optional<WarningService.WarningInput> dayBenefitWarning(final String municipalityId, final SsbtekIncome income) {
		final var readable = wholeMonth(income);
		if (readable && (income.days() == null)) {
			return warning(WarningService.TYPE_SSBTEK_DAY_CHECK, sourceKey("DAGERSATTNING", income),
				periodRulesService.dayCheck(municipalityId, true, null, null));
		}
		if (readable) {
			// The comparison needs ickeRodaDagar, which the solution has no source for — see the class javadoc.
			LOG.debug("Skipping the dagersättning day comparison for a payment covering {}: no non-red-day calendar",
				income.periodFrom());
			return Optional.empty();
		}
		return warning(WarningService.TYPE_SSBTEK_DAY_CHECK, sourceKey("DAGERSATTNING", income),
			periodRulesService.dayCheck(municipalityId, false, income.days(), null));
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
		return matches(income, DAY_BENEFIT_SUB_BENEFITS, DAY_BENEFIT_AMOUNT_TYPES);
	}

	private static boolean matches(final SsbtekIncome income, final Set<String> subBenefits, final Set<String> amountTypes) {
		return BENEFIT_DAY_ALLOWANCE.equals(normalize(income.benefit()))
			&& subBenefits.contains(normalize(income.subBenefit()))
			&& amountTypes.contains(normalize(income.amountType()));
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
	private static String sourceKey(final String rule, final SsbtekIncome income) {
		return rule + ":" + ofNullable(income.periodFrom()).map(LocalDate::toString).orElseGet(
			() -> ofNullable(income.period()).map(LocalDate::toString).orElse("okand-period"));
	}

	private static Optional<WarningService.WarningInput> warning(final String type, final String sourceKey,
		final PeriodRulesService.PeriodVerdict verdict) {

		if (!verdict.warning()) {
			return Optional.empty();
		}
		return Optional.of(new WarningService.WarningInput(type, sourceKey, verdict.rule()));
	}
}
