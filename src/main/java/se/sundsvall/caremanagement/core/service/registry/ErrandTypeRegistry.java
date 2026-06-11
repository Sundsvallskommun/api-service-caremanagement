package se.sundsvall.caremanagement.core.service.registry;

import java.util.Set;

/**
 * Read-only view of registered errand types.
 *
 * Type modules don't talk to this — they contribute via {@link ErrandTypeContribution}
 * beans. Core and other modules use this to validate slugs, look up displayable names,
 * and verify status transitions.
 */
public interface ErrandTypeRegistry {

	Set<String> knownSlugs();

	ErrandTypeContribution get(String typeSlug);

	boolean exists(String typeSlug);

	boolean isValidStatus(String typeSlug, String status);

	boolean isValidTransition(String typeSlug, String fromStatus, String toStatus);
}
