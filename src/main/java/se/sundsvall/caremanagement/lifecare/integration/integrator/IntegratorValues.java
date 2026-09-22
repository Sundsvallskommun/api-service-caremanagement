package se.sundsvall.caremanagement.lifecare.integration.integrator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * The type conversions every integrator mapper needs. The two models describe the same data, but the integrator gives
 * it proper types where FamilyCare renders it: amounts are {@link BigDecimal} rather than {@code Double}, and dates
 * are {@link LocalDate} / {@link OffsetDateTime} rather than the strings FamilyCare hands out.
 *
 * <p>
 * Going back to FamilyCare's shapes therefore means losing precision and structure, which is the right direction here:
 * the consumers downstream already parse those strings, and changing them would be a change to careM's own API rather
 * than a change of route.
 */
final class IntegratorValues {

	private IntegratorValues() {}

	static <S, T> List<T> mapEach(final List<S> source, final Function<S, T> mapper) {
		return ofNullable(source).orElseGet(List::of).stream().map(mapper).toList();
	}

	static Double toDouble(final BigDecimal value) {
		return ofNullable(value).map(BigDecimal::doubleValue).orElse(null);
	}

	static String toText(final LocalDate date) {
		return ofNullable(date).map(LocalDate::toString).orElse(null);
	}

	static String toText(final OffsetDateTime timestamp) {
		return ofNullable(timestamp).map(OffsetDateTime::toString).orElse(null);
	}

	static Integer toInteger(final Long value) {
		return ofNullable(value).map(Long::intValue).orElse(null);
	}

	/**
	 * FamilyCare identifies a few things by a plain integer where the integrator uses a string. A value that is not a
	 * number becomes {@code null} rather than an exception: one unparseable id must not fail the whole page of results.
	 */
	static Integer toInteger(final String value) {
		try {
			return ofNullable(value).map(Integer::valueOf).orElse(null);
		} catch (final NumberFormatException e) {
			return null;
		}
	}
}
