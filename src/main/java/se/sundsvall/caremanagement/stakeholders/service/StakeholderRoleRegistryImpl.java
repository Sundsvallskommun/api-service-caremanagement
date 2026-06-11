package se.sundsvall.caremanagement.stakeholders.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

@Component
class StakeholderRoleRegistryImpl implements StakeholderRoleRegistry {

	private final Map<String, Set<RoleDefinition>> rolesByType;
	private final Map<String, Set<String>> codesByType;

	StakeholderRoleRegistryImpl(final List<StakeholderRoleContribution> contributions) {
		this.rolesByType = contributions.stream()
			.collect(Collectors.toUnmodifiableMap(
				StakeholderRoleContribution::typeSlug,
				StakeholderRoleContribution::roles));
		this.codesByType = rolesByType.entrySet().stream()
			.collect(Collectors.toUnmodifiableMap(
				Map.Entry::getKey,
				e -> e.getValue().stream()
					.map(RoleDefinition::code)
					.collect(Collectors.toUnmodifiableSet())));
	}

	@Override
	public Set<RoleDefinition> rolesFor(final String typeSlug) {
		return Optional.ofNullable(rolesByType.get(typeSlug)).orElse(Set.of());
	}

	@Override
	public boolean isValidRole(final String typeSlug, final String role) {
		return Optional.ofNullable(codesByType.get(typeSlug))
			.map(codes -> codes.contains(role))
			.orElse(false);
	}

	@Override
	public Set<String> knownTypes() {
		return rolesByType.keySet();
	}
}
