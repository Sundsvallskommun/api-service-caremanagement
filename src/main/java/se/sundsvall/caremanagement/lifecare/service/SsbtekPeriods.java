package se.sundsvall.caremanagement.lifecare.service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The three SSBTEK rule periods for a normberäkning, derived from the month the application concerns
 * (ansökningsmånad). Per {@code vof-ekonomiskt-bistand/docs/ssbtek-regelverk.txt}:
 * <ul>
 * <li><b>ansökningsperiod</b> — the month the application is for (e.g. a May application made mid-April → May)</li>
 * <li><b>kontrollperiod</b> — the month before the ansökningsperiod (April)</li>
 * <li><b>jämförelseperiod</b> — the month before the kontrollperiod (March)</li>
 * </ul>
 *
 * @param ansokningsperiod the month the application concerns
 * @param kontrollperiod   the month before — the primary income source for the normberäkning
 * @param jamforelseperiod the month before that — used to fill gaps and to compare for change warnings
 */
public record SsbtekPeriods(YearMonth ansokningsperiod, YearMonth kontrollperiod, YearMonth jamforelseperiod) {

	/**
	 * Derive the periods from the ansökningsmånad.
	 *
	 * @param  applicationMonth the month the application concerns
	 * @return                  the three periods
	 */
	public static SsbtekPeriods forApplicationMonth(final YearMonth applicationMonth) {
		return new SsbtekPeriods(applicationMonth, applicationMonth.minusMonths(1), applicationMonth.minusMonths(2));
	}

	/** Whether a date falls in the kontrollperiod (the month before the application). */
	public boolean isInKontrollperiod(final LocalDate date) {
		return inMonth(date, kontrollperiod);
	}

	/** Whether a date falls in the jämförelseperiod (two months before the application). */
	public boolean isInJamforelseperiod(final LocalDate date) {
		return inMonth(date, jamforelseperiod);
	}

	private static boolean inMonth(final LocalDate date, final YearMonth month) {
		return (date != null) && YearMonth.from(date).equals(month);
	}
}
