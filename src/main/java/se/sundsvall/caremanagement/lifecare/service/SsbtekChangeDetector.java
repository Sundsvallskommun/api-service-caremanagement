package se.sundsvall.caremanagement.lifecare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.caremanagement.lifecare.service.mapper.SsbtekIncomeRegistry;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekChangeWarning;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

/**
 * Detects significant income changes between the jämförelseperiod and the kontrollperiod, per ssbtek-regelverk.txt
 * ("jämför summering i jämförelseperiod med summering i kontrollperiod. Om det skiljer mer än X% (12 idag) ner eller
 * upp
 * = varning"). For each förmån present in the jämförelseperiod it sums the net amounts there and in the kontrollperiod
 * and raises a {@link SsbtekChangeWarning} when the change exceeds the threshold in either direction (a förmån that
 * disappeared in the kontrollperiod is a −100% change). Net amounts are compared, matching the regelverk.
 *
 * <p>
 * The förmån-specific day-count checks (aktivitetsstöd/utvecklingsersättning/etableringsersättning and föräldrapenning
 * —
 * antal uttagna dagar vs icke-röda-dagar) are not implemented: they need fields (uttagna dagar, period day counts) that
 * are not present in any current SSBTEK sample payload.
 */
public final class SsbtekChangeDetector {

	/** The default change threshold in percent (12 % today, per the regelverk). */
	public static final BigDecimal DEFAULT_THRESHOLD_PERCENT = BigDecimal.valueOf(12);

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private SsbtekChangeDetector() {}

	/**
	 * Detect income changes using the default {@value #DEFAULT_THRESHOLD_PERCENT}% threshold.
	 *
	 * @param  incomes          the normalised incomes (covering both periods); may be {@code null}
	 * @param  applicationMonth the month the application concerns
	 * @return                  one warning per förmån whose net change exceeds the threshold
	 */
	public static List<SsbtekChangeWarning> detectIncomeChanges(final List<SsbtekIncome> incomes, final YearMonth applicationMonth) {
		return detectIncomeChanges(incomes, applicationMonth, DEFAULT_THRESHOLD_PERCENT);
	}

	/**
	 * Detect income changes using a caller-supplied threshold.
	 *
	 * @param  incomes          the normalised incomes (covering both periods); may be {@code null}
	 * @param  applicationMonth the month the application concerns
	 * @param  thresholdPercent the change threshold in percent (exclusive)
	 * @return                  one warning per förmån whose net change exceeds the threshold
	 */
	public static List<SsbtekChangeWarning> detectIncomeChanges(final List<SsbtekIncome> incomes, final YearMonth applicationMonth, final BigDecimal thresholdPercent) {
		final var periods = SsbtekPeriods.forApplicationMonth(applicationMonth);

		final var present = ofNullable(incomes).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.toList();

		final var kontrollSums = sumByForman(present.stream().filter(income -> periods.isInKontrollperiod(income.period())).toList());
		final var jamforelse = present.stream().filter(income -> periods.isInJamforelseperiod(income.period())).toList();
		final var jamforelseSums = sumByForman(jamforelse);
		final var displayNames = jamforelse.stream().collect(toMap(income -> SsbtekIncomeRegistry.normalize(income.forman()), SsbtekIncome::forman, (first, second) -> first));

		return jamforelseSums.entrySet().stream()
			.map(entry -> toWarning(displayNames.get(entry.getKey()), entry.getValue(), kontrollSums.getOrDefault(entry.getKey(), BigDecimal.ZERO)))
			.filter(Objects::nonNull)
			.filter(warning -> warning.changePercent().abs().compareTo(thresholdPercent) > 0)
			.toList();
	}

	private static Map<String, BigDecimal> sumByForman(final List<SsbtekIncome> incomes) {
		return incomes.stream()
			.filter(income -> income.netAmount() != null)
			.collect(groupingBy(income -> SsbtekIncomeRegistry.normalize(income.forman()),
				mapping(SsbtekIncome::netAmount, reducing(BigDecimal.ZERO, BigDecimal::add))));
	}

	private static SsbtekChangeWarning toWarning(final String forman, final BigDecimal jamforelseSum, final BigDecimal kontrollSum) {
		if (jamforelseSum.signum() == 0) {
			return null;
		}
		final var changePercent = kontrollSum.subtract(jamforelseSum).multiply(HUNDRED).divide(jamforelseSum, 1, RoundingMode.HALF_UP);
		return new SsbtekChangeWarning(forman, jamforelseSum, kontrollSum, changePercent);
	}
}
