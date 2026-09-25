package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.service.model.IncomeTypeTotal;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static java.util.Optional.ofNullable;

/**
 * Warns for the income types whose amount this month differs from the previous normberäkning — the
 * {@link WarningService#TYPE_INCOME_CHANGE} warnings.
 *
 * <p>
 * Verksamheten decided (G4) that <em>föregående månad</em> means the previous normberäkning in Lifecare — what the
 * handläggare actually counted last month — not the previous SSBTEK month. The comparison is therefore made per
 * Lifecare income type, the only unit the normberäkning knows: this month's side is what the transfer puts on the type
 * ({@code CalculationService.incomeTypeTotals}), the previous side the type's amount on the previous normberäkning.
 * Until 2026-09-25 the engine made this comparison between the SSBTEK comparison and control periods and sent the
 * result along; that list is no longer read.
 * </p>
 *
 * <p>
 * How much of a change is worth a warning is verksamhetens {@code Decision_inkomstTroskel}, asked with the SSBTEK
 * benefit that fed the type: {@code 0} compares the amounts exactly, {@code -1} does not compare at all, anything else
 * is a tolerance in percent of the previous amount. A type the previous normberäkning did not have, or had at zero, is
 * a new income rather
 * than a difference. A type only the previous normberäkning had is not this warning's business —
 * {@link WarningService#TYPE_MISSING_SSBTEK} already says it is missing.
 * </p>
 *
 * <p>
 * Every text starts with the type name and a colon, which is what {@code WarningService} keys the warning on, so a type
 * keeps one warning whether it is new or changed, refreshed while the difference lasts and auto-closed once it is gone.
 * </p>
 */
@Service
public class IncomeChangeFeeder {

	static final String DECISION_KEY = "Decision_inkomstTroskel";
	static final String NEW_INCOME_TEMPLATE = "%s: ny inkomst sedan föregående normberäkning, %s kr – kontrollera summan";
	static final String CHANGED_TEMPLATE = "%s: %s kr i föregående normberäkning → %s kr nu";
	static final String RULE_SEPARATOR = " – ";

	/** The threshold the table's own default row carries, used when the table cannot be asked. */
	static final int DEFAULT_THRESHOLD_PERCENT = 12;
	private static final int EXACT = 0;

	private static final String INPUT_BENEFIT = "forman";
	private static final String OUTPUT_THRESHOLD = "troskelProcent";
	private static final String OUTPUT_RULE = "regel";
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private static final Logger LOG = LoggerFactory.getLogger(IncomeChangeFeeder.class);

	private final ProcessService processService;

	IncomeChangeFeeder(final ProcessService processService) {
		this.processService = processService;
	}

	/**
	 * The threshold for one income type.
	 *
	 * @param percent the tolerance in percent of the previous amount — {@code 0} exact, negative no comparison
	 * @param rule    verksamhetens text for the warning, {@code null} when the table gave none
	 */
	record Threshold(int percent, String rule) {

		static Threshold defaults() {
			return new Threshold(DEFAULT_THRESHOLD_PERCENT, null);
		}
	}

	/**
	 * The income-change warnings for this month's transfer against the previous normberäkning.
	 *
	 * @param  municipalityId the municipality whose decision table is asked
	 * @param  current        this month's total per income type
	 * @param  previous       the previous normberäkning's amount per normalised income-type name; empty when there is
	 *                        no previous normberäkning (a nyansökan), which leaves nothing to compare with
	 * @return                the warning texts, in the order of {@code current}
	 */
	public List<String> incomeChangeWarnings(final String municipalityId, final List<IncomeTypeTotal> current, final Optional<Map<String, BigDecimal>> previous) {
		return changeWarnings(current, previous, total -> threshold(municipalityId, total.benefits()));
	}

	/**
	 * The comparison itself, with the thresholds supplied — kept free of the engine so it can be reasoned about on its
	 * own. A threshold is only asked for a type that is new or has changed.
	 */
	static List<String> changeWarnings(final List<IncomeTypeTotal> current, final Optional<Map<String, BigDecimal>> previous,
		final Function<IncomeTypeTotal, Threshold> thresholds) {
		return ofNullable(previous).flatMap(Function.identity())
			.map(previousAmounts -> ofNullable(current).orElseGet(List::of).stream()
				.filter(Objects::nonNull)
				.filter(total -> StringUtils.hasText(total.typeName()))
				.map(total -> warningFor(total, previousAmounts.get(total.key()), thresholds))
				.flatMap(Optional::stream)
				.toList())
			.orElseGet(List::of);
	}

	private static Optional<String> warningFor(final IncomeTypeTotal total, final BigDecimal previousAmount, final Function<IncomeTypeTotal, Threshold> thresholds) {
		final var currentAmount = ofNullable(total.amount()).orElse(BigDecimal.ZERO);
		final var previous = ofNullable(previousAmount).orElse(BigDecimal.ZERO);
		if (previous.signum() == 0) {
			return newIncome(total, currentAmount, thresholds);
		}
		if (currentAmount.compareTo(previous) == 0) {
			return Optional.empty();
		}
		final var threshold = thresholds.apply(total);
		if (!exceeds(previous, currentAmount, threshold)) {
			return Optional.empty();
		}
		final var text = CHANGED_TEMPLATE.formatted(total.typeName(), plain(previous), plain(currentAmount));
		return Optional.of(ofNullable(threshold.rule()).map(rule -> text + RULE_SEPARATOR + rule).orElse(text));
	}

	/** A type the previous normberäkning did not have, or had at zero: new, unless the table says not to compare it. */
	private static Optional<String> newIncome(final IncomeTypeTotal total, final BigDecimal currentAmount, final Function<IncomeTypeTotal, Threshold> thresholds) {
		if ((currentAmount.signum() <= 0) || (thresholds.apply(total).percent() < EXACT)) {
			return Optional.empty();
		}
		return Optional.of(NEW_INCOME_TEMPLATE.formatted(total.typeName(), plain(currentAmount)));
	}

	/**
	 * Whether a change from a non-zero previous amount is worth a warning. The exact threshold compares the amounts
	 * themselves, never a rounded percent — 1250 to 1255 kr is 0,4 % and must still warn. The percent threshold is
	 * checked without division, as {@code |change| * 100 > |previous| * threshold}.
	 */
	private static boolean exceeds(final BigDecimal previous, final BigDecimal current, final Threshold threshold) {
		if (threshold.percent() < EXACT) {
			return false;
		}
		if (threshold.percent() == EXACT) {
			return current.compareTo(previous) != 0;
		}
		final var change = current.subtract(previous).abs().multiply(HUNDRED);
		return change.compareTo(previous.abs().multiply(BigDecimal.valueOf(threshold.percent()))) > 0;
	}

	/**
	 * The threshold for the benefits that fed a type, asked with the first of them in sorted order. Best-effort: a
	 * table that cannot be asked, or that answers nothing, gives its own default row's 12 % and no text.
	 */
	private Threshold threshold(final String municipalityId, final List<String> benefits) {
		final var benefit = ofNullable(benefits).orElseGet(List::of).stream()
			.filter(StringUtils::hasText)
			.sorted()
			.findFirst();
		if (benefit.isEmpty()) {
			return Threshold.defaults();
		}
		try {
			final var rows = processService.evaluateDecision(municipalityId, DECISION_KEY, Map.of(INPUT_BENEFIT, benefit.get()));
			return ofNullable(rows).orElseGet(List::of).stream()
				.findFirst()
				.map(IncomeChangeFeeder::toThreshold)
				.orElseGet(Threshold::defaults);
		} catch (final RuntimeException e) {
			LOG.warn("Income threshold rules ({}) unavailable — comparing with the default {} %", DECISION_KEY, DEFAULT_THRESHOLD_PERCENT, e);
			return Threshold.defaults();
		}
	}

	private static Threshold toThreshold(final Map<String, Object> row) {
		final var percent = ofNullable(row.get(OUTPUT_THRESHOLD))
			.map(Object::toString)
			.map(value -> new BigDecimal(value.trim()).intValue())
			.orElse(DEFAULT_THRESHOLD_PERCENT);
		final var rule = ofNullable(row.get(OUTPUT_RULE))
			.map(Object::toString)
			.filter(StringUtils::hasText)
			.orElse(null);
		return new Threshold(percent, rule);
	}

	private static String plain(final BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}
}
