package se.sundsvall.caremanagement.lifecare.integration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.util.Optional.ofNullable;

/**
 * Renders dates in the format the Lifecare FamilyCare API accepts: RFC 3339 <em>with</em> a time component, e.g.
 * {@code 2020-01-01T00:00:00}. A bare {@code yyyy-MM-dd} is rejected with {@code 400 "… is not entered in a valid
 * format."}, and the {@code 00000000}/{@code 99999999} min/max sentinels are rejected on the person-based reads, so
 * every date leaving us for FamilyCare — read windows and write bodies alike — goes through here.
 *
 * <p>
 * Note that {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} cannot be used: it drops the seconds field when it is zero
 * ({@code 2020-01-01T00:00}), which FamilyCare rejects.
 */
public final class FamilyCareDates {

	private static final DateTimeFormatter FAMILY_CARE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private FamilyCareDates() {}

	/**
	 * The date at start of day — the inclusive lower bound of a read window, and the format every date written to
	 * FamilyCare (calculation and actualisation dates) uses.
	 *
	 * @param  date the date; may be {@code null}
	 * @return      the date as {@code yyyy-MM-dd'T'00:00:00}, or {@code null} when the date is {@code null}
	 */
	public static String startOfDay(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().format(FAMILY_CARE_DATE_TIME)).orElse(null);
	}

	/**
	 * The date at end of day — the inclusive upper bound of a read window, so records stamped later than midnight on
	 * the last day of the window are included.
	 *
	 * @param  date the date; may be {@code null}
	 * @return      the date as {@code yyyy-MM-dd'T'23:59:59}, or {@code null} when the date is {@code null}
	 */
	public static String endOfDay(final LocalDate date) {
		return ofNullable(date).map(value -> value.atTime(23, 59, 59).format(FAMILY_CARE_DATE_TIME)).orElse(null);
	}
}
