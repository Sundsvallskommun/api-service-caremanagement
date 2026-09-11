package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SwedishCalendarTest {

	@ParameterizedTest
	@CsvSource({
		// Easter drives five of the holidays, so it is worth pinning against the published dates
		"2024, 2024-03-31",
		"2025, 2025-04-20",
		"2026, 2026-04-05",
		"2027, 2027-03-28",
		"2028, 2028-04-16"
	})
	void derivesEasterSunday(final int year, final LocalDate easterSunday) {
		assertThat(SwedishCalendar.publicHolidays(year)).contains(easterSunday);
	}

	@Test
	void findsTheFloatingHolidaysInTheirWindows() {
		final var holidays2026 = SwedishCalendar.publicHolidays(2026);

		// midsommardagen is the Saturday falling 20-26 June; in 2026 that is the 20th
		assertThat(holidays2026).contains(LocalDate.of(2026, 6, 20));
		// alla helgons dag is the Saturday falling 31 Oct - 6 Nov; in 2026 that is the 31st
		assertThat(holidays2026).contains(LocalDate.of(2026, 10, 31));
	}

	@Test
	void coversTheFixedHolidays() {
		assertThat(SwedishCalendar.publicHolidays(2026)).contains(
			LocalDate.of(2026, 1, 1), // nyårsdagen
			LocalDate.of(2026, 1, 6), // trettondedag jul
			LocalDate.of(2026, 5, 1), // första maj
			LocalDate.of(2026, 6, 6), // nationaldagen
			LocalDate.of(2026, 12, 25), // juldagen
			LocalDate.of(2026, 12, 26)); // annandag jul
		assertThat(SwedishCalendar.publicHolidays(2026)).hasSize(13);
	}

	@ParameterizedTest
	@CsvSource({
		// September 2026 has 22 weekdays and no holidays
		"2026, 9, 22",
		// April 2026: 22 weekdays, minus långfredagen (3rd) and annandag påsk (6th)
		"2026, 4, 20",
		// December 2026: 23 weekdays, minus juldagen (Friday 25th); annandag jul falls on the Saturday
		"2026, 12, 22",
		// June 2026: 22 weekdays, minus nationaldagen falling on the Saturday - so nothing is deducted
		"2026, 6, 22",
		// January 2026: 22 weekdays, minus nyårsdagen (Thursday) and trettondedagen (Tuesday)
		"2026, 1, 20"
	})
	void countsTheWorkingDaysOfAMonth(final int year, final int month, final int expected) {
		assertThat(SwedishCalendar.workingDays(YearMonth.of(year, month))).isEqualTo(expected);
	}

	@Test
	void weekendHolidaysNeverChangeTheWorkingDayCount() {
		// påskdagen, midsommardagen and alla helgons dag always land on a Saturday or Sunday
		assertThat(SwedishCalendar.workingDays(YearMonth.of(2026, 11))).isEqualTo(21);
	}

	@Test
	void identifiesAPublicHolidayAndToleratesNoDate() {
		assertThat(SwedishCalendar.isPublicHoliday(LocalDate.of(2026, 6, 6))).isTrue();
		assertThat(SwedishCalendar.isPublicHoliday(LocalDate.of(2026, 6, 5))).isFalse();
		assertThat(SwedishCalendar.isPublicHoliday(null)).isFalse();
		assertThat(SwedishCalendar.workingDays(null)).isZero();
	}
}
