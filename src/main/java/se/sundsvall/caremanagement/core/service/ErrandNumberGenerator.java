package se.sundsvall.caremanagement.core.service;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandNumberSequenceEntity;

import static java.time.ZoneId.systemDefault;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

/**
 * Builds the human-readable errand number on the form {@code <PREFIX>-<YY><MM><sequence>} (e.g. {@code EB-26060071}).
 *
 * <ul>
 * <li>{@code PREFIX} is the namespace short code from {@link ErrandNumberPrefixResolver}; when none is configured it
 * falls back to the namespace in upper case, or {@code ERRAND} when even that is blank.</li>
 * <li>{@code YY}/{@code MM} are the two-digit creation year and month.</li>
 * <li>{@code sequence} is a per {@code (municipality, namespace, year, month)} running counter, zero-padded to four
 * digits and growing beyond that if needed. The month is part of the counter key, so numbering restarts every
 * month.</li>
 * </ul>
 *
 * Runs inside the creating transaction ({@link ErrandService#createErrand}); the counter is incremented under a
 * pessimistic write lock so concurrent creations never collide.
 */
@Component
class ErrandNumberGenerator {

	private static final String FALLBACK_PREFIX = "ERRAND";

	private final ErrandNumberPrefixResolver prefixResolver;
	private final ErrandNumberSequenceRepository sequenceRepository;

	ErrandNumberGenerator(final ErrandNumberPrefixResolver prefixResolver, final ErrandNumberSequenceRepository sequenceRepository) {
		this.prefixResolver = prefixResolver;
		this.sequenceRepository = sequenceRepository;
	}

	String generate(final String municipalityId, final String namespace) {
		final var today = LocalDate.now(systemDefault());
		final var year = today.getYear();
		final var month = today.getMonthValue();
		final var prefix = prefixResolver.resolvePrefix(municipalityId, namespace)
			.filter(StringUtils::hasText)
			.orElseGet(() -> fallbackPrefix(namespace));

		return "%s-%02d%02d%04d".formatted(prefix, year % 100, month, nextSequenceValue(municipalityId, namespace, year, month));
	}

	private long nextSequenceValue(final String municipalityId, final String namespace, final int year, final int month) {
		final var sequence = sequenceRepository.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(municipalityId, namespace, year, month)
			.orElseGet(() -> ErrandNumberSequenceEntity.create()
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withSequenceYear(year)
				.withSequenceMonth(month)
				.withCurrentValue(0L));

		final var next = sequence.getCurrentValue() + 1;
		sequenceRepository.save(sequence.withCurrentValue(next));
		return next;
	}

	private static String fallbackPrefix(final String namespace) {
		return ofNullable(namespace)
			.filter(not(String::isBlank))
			.map(value -> value.toUpperCase(ROOT))
			.orElse(FALLBACK_PREFIX);
	}
}
