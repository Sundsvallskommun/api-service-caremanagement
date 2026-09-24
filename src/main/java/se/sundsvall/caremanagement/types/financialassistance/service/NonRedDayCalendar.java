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
 * <strong>What is red.</strong> Every Saturday and Sunday, plus the allmänna helgdagar of lag (1989:253): nyårsdagen,
 * trettondedag jul, långfredagen, påskdagen, annandag påsk, första maj, Kristi himmelsfärds dag, pingstdagen,
 * nationaldagen, midsommardagen (the Saturday 20–26 June), alla helgons dag (the Saturday 31 October – 6 November),
 * juldagen and annandag jul. Computed with {@code java.time} — no external calendar, nothing to operate.
 * </p>
 *
 * <p>
 * <strong>Saturdays are red.</strong> Aktivitetsstöd, utvecklings- and etableringsersättning are paid for at most five
 * days per calendar week (förordning 2017:819, 10 kap. 6 §), so a count that included Saturdays is one no correct
 * payment can reach, and the check would warn on every one of them. The literal "icke-röda dagar" of the decision is
 * read as weekdays that are not helgdagar; the question is put to verksamheten for confirmation, not left to block.
 * </p>
 *
 * <p>
 * <strong>The eves (svar 2026-09-24 §1, kompletterande besked 2026-09-23).</strong> Julafton, nyårsafton and
 * midsommarafton are not helgdagar in the law, and verksamheten has decided all three are ordinary ersättningsdagar:
 * they count as non-red unless they fall on a weekend. The check was cross-read against a real payment for a whole
 * June, which was for 22 days — June 2026's count with midsommarafton as an ordinary day.
 * </p>
 *
 * <p>
 * <strong>{@link #EVE_READING} records that decision.</strong> While midsommarafton was open it stood at
 * {@link EveReading#UNDECIDED}, returning both readings so the table accepted either. Decided, both counts are the same
 * number and the table's tolerance input ({@code ickeRodaDagarAlternativ}) simply repeats {@code ickeRodaDagar}; it is
 * kept so the published DMN does not have to change with the answer.
 * </p>
 */
final class NonRedDayCalendar {

	/** How midsommarafton is read. */
	enum EveReading {
		/** Verksamheten has not decided: accept a day count that matches either reading. */
		UNDECIDED,
		/** Midsommarafton is red. */
		EVE_IS_RED,
		/** Midsommarafton is an ordinary (non-red) day. */
		EVE_IS_NOT_RED
	}

	/**
	 * Midsommarafton is an ersättningsdag: verksamhetens kompletterande besked 2026-09-23, recorded in svar 2026-09-24
	 * §1.
	 */
	static final EveReading EVE_READING = EveReading.EVE_IS_NOT_RED;

	/**
	 * The non-red day counts a payment's day count is accepted against.
	 *
	 * @param count            the count with midsommarafton as an ordinary day, or the decided reading's count
	 * @param alternativeCount the count with midsommarafton as a red day while undecided, or the decided reading's count
	 *                         again
	 */
	record NonRedDays(int count, int alternativeCount) {}

	private NonRedDayCalendar() {}

	/** The month's non-red days under {@link #EVE_READING}. */
	static NonRedDays nonRedDays(final YearMonth month) {
		return nonRedDays(month, EVE_READING);
	}

	static NonRedDays nonRedDays(final YearMonth month, final EveReading reading) {
		final var eveOrdinary = count(month, false);
		final var eveRed = count(month, true);
		return switch (reading) {
			case EVE_IS_RED -> new NonRedDays(eveRed, eveRed);
			case EVE_IS_NOT_RED -> new NonRedDays(eveOrdinary, eveOrdinary);
			case UNDECIDED -> new NonRedDays(eveOrdinary, eveRed);
		};
	}

	private static int count(final YearMonth month, final boolean eveRed) {
		final var holidays = publicHolidays(month.getYear());
		final Set<LocalDate> eves;
		if (eveRed) {
			eves = Set.of(midsummerEve(month.getYear()));
		} else {
			eves = Set.of();
		}
		return (int) month.atDay(1).datesUntil(month.atEndOfMonth().plusDays(1))
			.filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY)
			.filter(date -> !holidays.contains(date))
			.filter(date -> !eves.contains(date))
			.count();
	}

	/**
	 * The working day that falls {@code workingDays} working days after {@code from} — {@code from} itself not counted.
	 * A working day is a weekday that is neither an allmän helgdag nor one of the three aftnar the municipality closes on
	 * (midsommar-, jul- and nyårsafton). This is the caseworker's calendar, not the benefit day count above: that one
	 * counts jul- and nyårsafton as ersättningsdagar by verksamhetens beslut, which says nothing about office days.
	 */
	static LocalDate plusWorkingDays(final LocalDate from, final int workingDays) {
		var date = from;
		var remaining = workingDays;
		while (remaining > 0) {
			date = date.plusDays(1);
			if (isWorkingDay(date)) {
				remaining--;
			}
		}
		return date;
	}

	private static boolean isWorkingDay(final LocalDate date) {
		final var year = date.getYear();
		return date.getDayOfWeek() != DayOfWeek.SATURDAY
			&& date.getDayOfWeek() != DayOfWeek.SUNDAY
			&& !publicHolidays(year).contains(date)
			&& !date.equals(midsummerEve(year))
			&& !date.equals(LocalDate.of(year, 12, 24))
			&& !date.equals(LocalDate.of(year, 12, 31));
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

	/** Midsommarafton, the Friday 19–25 June — a de facto day off, not a helgdag in the law. */
	static LocalDate midsummerEve(final int year) {
		return midsummerDay(year).minusDays(1);
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
