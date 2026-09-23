package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The number of icke-röda dagar in a month, for the dagersättning day check
 * ({@code Decision_dagersattningDagkontroll}): verksamheten compares the days an aktivitetsstöd / utvecklings- /
 * etableringsersättning payment is for against the non-red days of the month the payment covers (svar 2026-09-23 §2).
 *
 * <p>
 * <strong>What is red.</strong> Every Sunday, plus the allmänna helgdagar of lag (1989:253): nyårsdagen, trettondedag
 * jul, långfredagen, påskdagen, annandag påsk, första maj, Kristi himmelsfärds dag, pingstdagen, nationaldagen,
 * midsommardagen (the Saturday 20–26 June), alla helgons dag (the Saturday 31 October – 6 November), juldagen and
 * annandag jul. Computed with {@code java.time} — no external calendar, nothing to operate.
 * </p>
 *
 * <p>
 * <strong>Saturdays that are not a helgdag count as non-red.</strong> That is the literal reading of "icke-röda dagar"
 * in the decision, and nothing in the regelverk says otherwise (it says "icke-röda-dagar" in one sentence and "antal
 * dagar i månaden" in the next). Should verksamheten mean weekdays, add a Saturday filter in {@link #count}.
 * </p>
 *
 * <p>
 * <strong>The open question — julafton, midsommarafton, nyårsafton.</strong> They are not helgdagar in the law but are
 * de facto days off, and verksamheten has not said whether they count as red. Until they do, the calendar does not
 * guess: {@link #nonRedDays(YearMonth)} returns the count under <em>both</em> readings and the table accepts a day
 * count that matches either, so the warning is only raised when the payment disagrees with both. In a month without an
 * eve on a non-red day the two counts are identical and the check is exact.
 * </p>
 *
 * <p>
 * <strong>When the answer comes, change {@link #EVE_READING} — nothing else.</strong> {@link EveReading#EVES_ARE_RED}
 * or {@link EveReading#EVES_ARE_NOT_RED} makes both counts the same number, and the tolerance disappears everywhere.
 * </p>
 */
final class NonRedDayCalendar {

	/** How julafton, midsommarafton and nyårsafton are read. */
	enum EveReading {
		/** Verksamheten has not decided: accept a day count that matches either reading. */
		UNDECIDED,
		/** The three eves are red. */
		EVES_ARE_RED,
		/** The three eves are ordinary (non-red) days unless they fall on a Sunday. */
		EVES_ARE_NOT_RED
	}

	/**
	 * THE switch for the open eve question (verksamhetens svar 2026-09-23 §2, "Fortfarande öppet"). Set it to the answer
	 * once it is given.
	 */
	static final EveReading EVE_READING = EveReading.UNDECIDED;

	/**
	 * The non-red day counts a payment's day count is accepted against.
	 *
	 * @param count            the count with the eves as ordinary days, or the decided reading's count
	 * @param alternativeCount the count with the eves as red days, or the decided reading's count again
	 */
	record NonRedDays(int count, int alternativeCount) {}

	private NonRedDayCalendar() {}

	/** The month's non-red days under {@link #EVE_READING}. */
	static NonRedDays nonRedDays(final YearMonth month) {
		return nonRedDays(month, EVE_READING);
	}

	static NonRedDays nonRedDays(final YearMonth month, final EveReading reading) {
		final var evesOrdinary = count(month, false);
		final var evesRed = count(month, true);
		return switch (reading) {
			case EVES_ARE_RED -> new NonRedDays(evesRed, evesRed);
			case EVES_ARE_NOT_RED -> new NonRedDays(evesOrdinary, evesOrdinary);
			case UNDECIDED -> new NonRedDays(evesOrdinary, evesRed);
		};
	}

	private static int count(final YearMonth month, final boolean evesRed) {
		final var holidays = publicHolidays(month.getYear());
		final Set<LocalDate> eves;
		if (evesRed) {
			eves = eves(month.getYear());
		} else {
			eves = Set.of();
		}
		return (int) month.atDay(1).datesUntil(month.atEndOfMonth().plusDays(1))
			.filter(date -> date.getDayOfWeek() != DayOfWeek.SUNDAY)
			.filter(date -> !holidays.contains(date))
			.filter(date -> !eves.contains(date))
			.count();
	}

	/**
	 * The allmänna helgdagar of lag (1989:253) for a year. Collected into a set rather than {@code Set.of}: two of them
	 * can coincide (Kristi himmelsfärd falls on 1 May when Easter is 23 March, e.g. 2008; pingstdagen on 6 June when
	 * Easter is 18 April).
	 */
	static Set<LocalDate> publicHolidays(final int year) {
		final var easter = easterSunday(year);
		return Stream.of(
			LocalDate.of(year, 1, 1), // nyårsdagen
			LocalDate.of(year, 1, 6), // trettondedag jul
			easter.minusDays(2), // långfredagen
			easter, // påskdagen
			easter.plusDays(1), // annandag påsk
			LocalDate.of(year, 5, 1), // första maj
			easter.plusDays(39), // Kristi himmelsfärds dag
			easter.plusDays(49), // pingstdagen
			LocalDate.of(year, 6, 6), // Sveriges nationaldag
			midsummerDay(year), // midsommardagen
			LocalDate.of(year, 10, 31).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)), // alla helgons dag
			LocalDate.of(year, 12, 25), // juldagen
			LocalDate.of(year, 12, 26)) // annandag jul
			.collect(Collectors.toUnmodifiableSet());
	}

	/** Julafton, midsommarafton and nyårsafton — de facto days off, not helgdagar in the law. */
	static Set<LocalDate> eves(final int year) {
		return Set.of(
			LocalDate.of(year, 12, 24), // julafton
			midsummerDay(year).minusDays(1), // midsommarafton, the Friday 19–25 June
			LocalDate.of(year, 12, 31)); // nyårsafton
	}

	private static LocalDate midsummerDay(final int year) {
		return LocalDate.of(year, 6, 20).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
	}

	/** Easter Sunday by the anonymous Gregorian algorithm (Meeus/Jones/Butcher). */
	static LocalDate easterSunday(final int year) {
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
