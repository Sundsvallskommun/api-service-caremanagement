package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.EveReading.EVE_IS_NOT_RED;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.EveReading.EVE_IS_RED;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.EveReading.UNDECIDED;

/**
 * Every expected count below was computed independently of this class (python-dateutil's Easter plus the lag
 * 1989:253 list), not read back from it.
 */
class NonRedDayCalendarTest {

	@ParameterizedTest(name = "Easter {0} is {1}")
	@CsvSource({
		"2008, 2008-03-23",
		"2024, 2024-03-31",
		"2025, 2025-04-20",
		"2026, 2026-04-05",
		"2027, 2027-03-28",
		"2028, 2028-04-16",
		"2038, 2038-04-25",
		"2285, 2285-03-22"
	})
	void easterSunday(final int year, final LocalDate expected) {
		assertThat(NonRedDayCalendar.easterSunday(year)).isEqualTo(expected);
	}

	@Test
	void publicHolidaysOf2026() {
		assertThat(NonRedDayCalendar.publicHolidays(2026)).containsExactlyInAnyOrder(
			LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 5),
			LocalDate.of(2026, 4, 6), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 24),
			LocalDate.of(2026, 6, 6), LocalDate.of(2026, 6, 20), LocalDate.of(2026, 10, 31), LocalDate.of(2026, 12, 25),
			LocalDate.of(2026, 12, 26));
	}

	@Test
	void coincidingHolidaysDoNotBreakTheSet() {
		// Easter 23 March 2008 puts Kristi himmelsfärd on 1 May - Set.of would throw on the duplicate.
		assertThat(NonRedDayCalendar.publicHolidays(2008)).hasSize(12).contains(LocalDate.of(2008, 5, 1));
	}

	@Test
	void midsummerEveIsTheFridayBeforeMidsommardagen() {
		assertThat(NonRedDayCalendar.midsummerEve(2026)).isEqualTo(LocalDate.of(2026, 6, 19));
		assertThat(NonRedDayCalendar.midsummerEve(2025)).isEqualTo(LocalDate.of(2025, 6, 20));
	}

	@ParameterizedTest(name = "{0}: {1} with midsommarafton ordinary, {2} with it red")
	@CsvSource({
		"2026-04, 20, 20", // långfredag + annandag påsk on weekdays, påskdagen on a Sunday
		"2026-06, 22, 21", // nationaldagen + midsommardagen on Saturdays, midsommarafton Friday 19th
		"2026-08, 21, 21", // no helgdag: 31 days, five Saturdays and five Sundays
		"2026-10, 22, 22", // alla helgons dag on Saturday 31 October
		"2026-12, 22, 22", // juldagen on Friday, annandag jul on Saturday; julafton and nyårsafton (Thursdays) are ersättningsdagar
		"2027-12, 23, 23", // annandag jul on a Sunday counts once
		"2025-06, 20, 19", // midsommarafton Friday 20th
		"2025-12, 21, 21", // the month verksamheten was asked about: 23 weekdays, 21 without helgdagar; the eves count (svar 2026-09-24)
		"2008-05, 21, 21" // Kristi himmelsfärd on första maj counts once
	})
	void undecidedGivesBothReadings(final String month, final int ordinary, final int red) {
		final var result = NonRedDayCalendar.nonRedDays(YearMonth.parse(month), UNDECIDED);

		assertThat(result.count()).isEqualTo(ordinary);
		assertThat(result.alternativeCount()).isEqualTo(red);
	}

	@Test
	void aDecidedReadingCollapsesToOneNumber() {
		final var june = YearMonth.of(2026, 6);

		assertThat(NonRedDayCalendar.nonRedDays(june, EVE_IS_RED)).isEqualTo(new NonRedDayCalendar.NonRedDays(21, 21));
		assertThat(NonRedDayCalendar.nonRedDays(june, EVE_IS_NOT_RED)).isEqualTo(new NonRedDayCalendar.NonRedDays(22, 22));
	}

	@Test
	void theSwitchIsStillUndecided() {
		// Fails on purpose the day someone flips EVE_READING, so the verksamhet's answer is recorded in the tests too.
		assertThat(NonRedDayCalendar.EVE_READING).isEqualTo(UNDECIDED);
		assertThat(NonRedDayCalendar.nonRedDays(YearMonth.of(2026, 6))).isEqualTo(new NonRedDayCalendar.NonRedDays(22, 21));
		// Julafton and nyårsafton are decided as ersättningsdagar: December has one exact count.
		assertThat(NonRedDayCalendar.nonRedDays(YearMonth.of(2026, 12))).isEqualTo(new NonRedDayCalendar.NonRedDays(22, 22));
	}
}
