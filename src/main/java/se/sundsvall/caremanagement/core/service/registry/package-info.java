/**
 * Exposed so type modules can contribute an {@link ErrandTypeContribution} bean
 * (slug, display name, allowed statuses and transitions) to the {@link ErrandTypeRegistry}.
 */
@NamedInterface("registry")
package se.sundsvall.caremanagement.core.service.registry;

import org.springframework.modulith.NamedInterface;
