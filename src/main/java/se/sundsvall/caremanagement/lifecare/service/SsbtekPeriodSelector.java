package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.mapper.SsbtekIncomeRegistry;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

/**
 * Applies the SSBTEK transfer rule (ssbtek-regelverk.txt, "Hantering av inkomster"): the incomes that should be carried
 * into the normberäkning for a given ansökningsmånad are
 * <ul>
 * <li>all incomes in the <b>kontrollperiod</b>, plus</li>
 * <li>incomes in the <b>jämförelseperiod</b> that are not already covered — i.e. whose förmån has no income in the
 * kontrollperiod ("inkomster i jämförelseperioden som inte redan är överförda ska överföras").</li>
 * </ul>
 *
 * <p>
 * "Redan överförda" is read here as a within-run dedup by förmån (kontroll is primary; jämförelse fills gaps). If it is
 * later confirmed to also mean "transferred in the previous month's normberäkning", that prior set would be an
 * additional input. Incomes outside both periods, or with no date, are not selected. The <em>which förmåner transfer at
 * all</em> decision stays with {@code SsbtekToFcIncomeMapper}/{@code SsbtekIncomeRegistry}; this only does period
 * selection.
 */
public final class SsbtekPeriodSelector {

	private SsbtekPeriodSelector() {}

	/**
	 * Select the incomes to transfer into the normberäkning for the given ansökningsmånad.
	 *
	 * @param  incomes          the normalised incomes (e.g. from {@code SsbtekIncomeExtractor}); may be {@code null}
	 * @param  applicationMonth the month the application concerns
	 * @return                  the kontrollperiod incomes plus the non-duplicated jämförelseperiod incomes
	 */
	public static List<SsbtekIncome> selectTransferable(final List<SsbtekIncome> incomes, final YearMonth applicationMonth) {
		final var periods = SsbtekPeriods.forApplicationMonth(applicationMonth);

		final var present = ofNullable(incomes).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.toList();

		final var kontroll = present.stream()
			.filter(income -> periods.isInKontrollperiod(income.period()))
			.toList();

		final Set<String> kontrollFormaner = kontroll.stream()
			.map(income -> SsbtekIncomeRegistry.normalize(income.forman()))
			.collect(toSet());

		final var jamforelseExtra = present.stream()
			.filter(income -> periods.isInJamforelseperiod(income.period()))
			.filter(income -> !kontrollFormaner.contains(SsbtekIncomeRegistry.normalize(income.forman())))
			.toList();

		return Stream.concat(kontroll.stream(), jamforelseExtra.stream()).toList();
	}
}
