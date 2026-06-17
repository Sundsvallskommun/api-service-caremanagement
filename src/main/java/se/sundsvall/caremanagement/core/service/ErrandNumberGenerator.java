package se.sundsvall.caremanagement.core.service;

import java.time.Year;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandNumberSequenceEntity;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

/**
 * Builds the human-readable errand number on the form {@code <PREFIX>_<year>_<sequence>} (e.g. {@code EB_2026_0001}).
 *
 * <ul>
 * <li>{@code PREFIX} is the namespace short code from {@link ErrandNumberPrefixResolver}; when none is configured it
 * falls back to the namespace in upper case, or {@code ERRAND} when even that is blank.</li>
 * <li>{@code sequence} is a per {@code (municipality, namespace, year)} running counter, zero-padded to four digits and
 * growing beyond that if needed. The year is part of the counter key, so numbering restarts every January.</li>
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
		final var year = Year.now().getValue();
		final var prefix = prefixResolver.resolvePrefix(municipalityId, namespace)
			.filter(StringUtils::hasText)
			.orElseGet(() -> fallbackPrefix(namespace));

		return "%s_%d_%04d".formatted(prefix, year, nextSequenceValue(municipalityId, namespace, year));
	}

	private long nextSequenceValue(final String municipalityId, final String namespace, final int year) {
		final var sequence = sequenceRepository.findByMunicipalityIdAndNamespaceAndSequenceYear(municipalityId, namespace, year)
			.orElseGet(() -> ErrandNumberSequenceEntity.create()
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withSequenceYear(year)
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
