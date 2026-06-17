package se.sundsvall.caremanagement.core.service;

import java.util.Optional;

/**
 * SPI for resolving the prefix segment of an errand number (e.g. {@code EB} in {@code EB-26060071}).
 *
 * Errand numbering is a core concern, but the prefix comes from the namespace's short code, which the core module
 * must not reach into directly. The {@code namespaceconfig} module provides the implementation; core depends only on
 * this abstraction (dependency inversion), keeping the module boundary intact.
 *
 * @see ErrandNumberGenerator
 */
public interface ErrandNumberPrefixResolver {

	/**
	 * @return the configured short code for the namespace, or empty when none is configured — in which case the
	 *         generator falls back to a prefix derived from the namespace itself.
	 */
	Optional<String> resolvePrefix(String municipalityId, String namespace);
}
