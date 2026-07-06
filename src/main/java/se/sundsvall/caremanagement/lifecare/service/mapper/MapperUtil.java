package se.sundsvall.caremanagement.lifecare.service.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.util.Optional.ofNullable;

/**
 * Small shared helpers for the lifecare FC mappers — pulled out of the individual mappers where they were duplicated.
 */
public final class MapperUtil {

	private MapperUtil() {}

	/** Trim + lower-case a value for case/space-insensitive name matching; {@code null} becomes {@code ""}. */
	public static String normalize(final String value) {
		return ofNullable(value).map(v -> v.trim().toLowerCase()).orElse("");
	}

	/** A {@link LocalDate} at UTC start-of-day, or {@code null} when the date is {@code null}. */
	public static OffsetDateTime toOffsetDateTime(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().atOffset(ZoneOffset.UTC)).orElse(null);
	}
}
