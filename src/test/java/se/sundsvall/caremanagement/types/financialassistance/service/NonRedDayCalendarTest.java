package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.EveReading.EVES_ARE_NOT_RED;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.EveReading.EVES_ARE_RED;
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
	void evesOf2026() {
		assertThat(NonRedDayCalendar.eves(2026)).containsExactlyInAnyOrder(
			LocalDate.of(2026, 6, 19), LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 31));
	}

	@ParameterizedTest(name = "{0}: {1} with the eves ordinary, {2} with the eves red")
	@CsvSource({
		"2026-04, 24, 24", // långfredag + annandag påsk on weekdays, påskdagen on a Sunday
		"2026-06, 24, 23", // nationaldagen + midsommardagen on Saturdays, midsommarafton Friday 19th
		"2026-08, 26, 26", // no helgdag: 31 days, five Sundays
		"2026-10, 26, 26", // alla helgons dag on Saturday 31 October
		"2026-12, 25, 23", // juldagen + annandag jul; julafton and nyårsafton both Thursdays
		"2027-12, 26, 24", // annandag jul on a Sunday counts once
		"2025-06, 23, 22",
		"2008-05, 26, 26" // Kristi himmelsfärd on första maj counts once
	})
	void undecidedGivesBothReadings(final String month, final int ordinary, final int red) {
		final var result = NonRedDayCalendar.nonRedDays(YearMonth.parse(month), UNDECIDED);

		assertThat(result.count()).isEqualTo(ordinary);
		assertThat(result.alternativeCount()).isEqualTo(red);
	}

	@Test
	void aDecidedReadingCollapsesToOneNumber() {
		final var december = YearMonth.of(2026, 12);

		assertThat(NonRedDayCalendar.nonRedDays(december, EVES_ARE_RED)).isEqualTo(new NonRedDayCalendar.NonRedDays(23, 23));
		assertThat(NonRedDayCalendar.nonRedDays(december, EVES_ARE_NOT_RED)).isEqualTo(new NonRedDayCalendar.NonRedDays(25, 25));
	}

	@Test
	void theSwitchIsStillUndecided() {
		// Fails on purpose the day someone flips EVE_READING, so the verksamhet's answer is recorded in the tests too.
		assertThat(NonRedDayCalendar.EVE_READING).isEqualTo(UNDECIDED);
		assertThat(NonRedDayCalendar.nonRedDays(YearMonth.of(2026, 12))).isEqualTo(new NonRedDayCalendar.NonRedDays(25, 23));
	}
}
