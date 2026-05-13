package se.sundsvall.caremanagement.stakeholders.service;

import java.util.Set;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

/**
 * Each type module exposes one of these as a Spring bean to declare its valid
 * stakeholder roles. The {@code stakeholders} module aggregates them at startup.
 */
public record StakeholderRoleContribution(
	String typeSlug,
	Set<RoleDefinition> roles) {
	public StakeholderRoleContribution {
		if (typeSlug == null || typeSlug.isBlank()) {
			throw new IllegalArgumentException("typeSlug must not be blank");
		}
		roles = Set.copyOf(roles);
	}
}
