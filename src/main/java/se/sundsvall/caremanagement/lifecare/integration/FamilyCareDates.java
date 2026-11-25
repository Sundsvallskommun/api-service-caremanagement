package se.sundsvall.caremanagement.lifecare.integration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.util.Optional.ofNullable;

/**
 * Renders the dates written to FamilyCare (calculation and actualisation dates) with a time component, e.g.
 * {@code 2020-01-01T00:00:00}. {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} cannot be used: it drops the seconds
 * field when it is zero ({@code 2020-01-01T00:00}).
 */
public final class FamilyCareDates {

	private static final DateTimeFormatter FAMILY_CARE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private FamilyCareDates() {}

	/**
	 * The date at start of day.
	 *
	 * @param  date the date; may be {@code null}
	 * @return      the date as {@code yyyy-MM-dd'T'00:00:00}, or {@code null} when the date is {@code null}
	 */
	public static String startOfDay(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().format(FAMILY_CARE_DATE_TIME)).orElse(null);
	}
}
