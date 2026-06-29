package se.sundsvall.caremanagement.stakeholders.service;

import java.util.Set;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

public interface StakeholderRoleRegistry {

	Set<RoleDefinition> rolesFor(String typeSlug);

	boolean isValidRole(String typeSlug, String role);

	Set<String> knownTypes();
}
