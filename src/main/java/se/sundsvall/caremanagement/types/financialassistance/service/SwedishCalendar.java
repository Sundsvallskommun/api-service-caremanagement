package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Swedish public holidays, and the working-day count the regelverk's day checks compare against.
 * <p>
 * The aktivitetsstöd rule reads "kontrollera antal icke-röda-dagar månaden som ersättningen avser". Nothing in the
 * solution knew which days those are, which is what kept {@code Decision_dagersattningDagkontroll} from being usable.
 * The dates are not something to obtain from anywhere - they are derivable, so they are derived here rather than
 * pulled in as a dependency or a service call.
 * <p>
 * <b>What counts as a red day.</b> {@link #workingDays(YearMonth)} counts Monday to Friday and subtracts the public
 * holidays that fall on those days. That matches how aktivitetsstöd is paid - at most five days a week - and is the
 * reading used until verksamheten says otherwise. The stricter calendar reading, where a red day is a Sunday or a
 * public holiday and Saturdays are ordinary, would give a larger number; {@link #publicHolidays(int)} is exposed so
 * that count can be built without touching this class.
 * <p>
 * Midsommardagen, alla helgons dag and påskdagen always fall on a Saturday or Sunday, so they never change the
 * working-day count. They are included because the holiday set is also the answer to "is this date a red day".
 */
public final class SwedishCalendar {

	private SwedishCalendar() {}

	/**
	 * The number of days in the month that are Monday to Friday and not a public holiday.
	 *
	 * @param  month the month to count
	 * @return       the working-day count, or 0 when {@code month} is {@code null}
	 */
	public static int workingDays(final YearMonth month) {
		if (month == null) {
			return 0;
		}
		final var holidays = publicHolidays(month.getYear());
		return (int) IntStream.rangeClosed(1, month.lengthOfMonth())
			.mapToObj(month::atDay)
			.filter(SwedishCalendar::isWeekday)
			.filter(date -> !holidays.contains(date))
			.count();
	}

	/**
	 * Whether the date is a public holiday - the red days proper, without Sundays.
	 *
	 * @param  date the date to test
	 * @return      {@code true} when the date is a Swedish public holiday
	 */
	public static boolean isPublicHoliday(final LocalDate date) {
		return (date != null) && publicHolidays(date.getYear()).contains(date);
	}

	/**
	 * The Swedish public holidays of a year.
	 *
	 * @param  year the year
	 * @return      the holiday dates
	 */
	public static Set<LocalDate> publicHolidays(final int year) {
		final var easterSunday = easterSunday(year);
		return Stream.of(
			LocalDate.of(year, Month.JANUARY, 1), // nyårsdagen
			LocalDate.of(year, Month.JANUARY, 6), // trettondedag jul
			easterSunday.minusDays(2), // långfredagen
			easterSunday, // påskdagen
			easterSunday.plusDays(1), // annandag påsk
			LocalDate.of(year, Month.MAY, 1), // första maj
			easterSunday.plusDays(39), // Kristi himmelsfärdsdag
			easterSunday.plusDays(49), // pingstdagen
			LocalDate.of(year, Month.JUNE, 6), // Sveriges nationaldag
			saturdayWithin(LocalDate.of(year, Month.JUNE, 20)), // midsommardagen, lördagen 20-26 juni
			saturdayWithin(LocalDate.of(year, Month.OCTOBER, 31)), // alla helgons dag, lördagen 31 okt - 6 nov
			LocalDate.of(year, Month.DECEMBER, 25), // juldagen
			LocalDate.of(year, Month.DECEMBER, 26)) // annandag jul
			.collect(Collectors.toUnmodifiableSet());
	}

	private static boolean isWeekday(final LocalDate date) {
		return (date.getDayOfWeek() != DayOfWeek.SATURDAY) && (date.getDayOfWeek() != DayOfWeek.SUNDAY);
	}

	/** The Saturday on or after the given date - both floating holidays are defined as a Saturday in a fixed window. */
	private static LocalDate saturdayWithin(final LocalDate windowStart) {
		return windowStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
	}

	/**
	 * Easter Sunday by the anonymous Gregorian algorithm. Five of the thirteen holidays are defined relative to it, so
	 * it has to be computed rather than tabulated.
	 */
	private static LocalDate easterSunday(final int year) {
		final var a = year % 19;
		final var b = year / 100;
		final var c = year % 100;
		final var d = b / 4;
		final var e = b % 4;
		final var f = (b + 8) / 25;
		final var g = (b - f + 1) / 3;
		final var h = ((19 * a) + b - d - g + 15) % 30;
		final var i = c / 4;
		final var k = c % 4;
		final var l = (32 + (2 * e) + (2 * i) - h - k) % 7;
		final var m = (a + (11 * h) + (22 * l)) / 451;
		final var month = (h + l - (7 * m) + 114) / 31;
		final var day = ((h + l - (7 * m) + 114) % 31) + 1;
		return LocalDate.of(year, month, day);
	}
}
