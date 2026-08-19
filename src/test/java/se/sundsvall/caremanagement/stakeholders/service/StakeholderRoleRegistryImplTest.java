package se.sundsvall.caremanagement.stakeholders.service;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class StakeholderRoleRegistryImplTest {

	private static StakeholderRoleContribution fostercare() {
		return new StakeholderRoleContribution("fostercare",
			Set.of(new RoleDefinition("FOSTER_PARENT", "Familjehemsförälder", 2, true)));
	}

	private static StakeholderRoleContribution adoption() {
		return new StakeholderRoleContribution("adoption",
			Set.of(new RoleDefinition("APPLICANT", "Sökande", 2, true)));
	}

	@Test
	void rolesForReturnsRegisteredRoles() {
		final var registry = new StakeholderRoleRegistryImpl(List.of(fostercare(), adoption()));
		assertThat(registry.rolesFor("fostercare"))
			.extracting(RoleDefinition::code)
			.containsExactly("FOSTER_PARENT");
	}

	@Test
	void rolesForUnknownTypeReturnsEmpty() {
		final var registry = new StakeholderRoleRegistryImpl(List.of(fostercare()));
		assertThat(registry.rolesFor("unknown")).isEmpty();
	}

	@Test
	void isValidRoleTrueForRegistered() {
		final var registry = new StakeholderRoleRegistryImpl(List.of(fostercare()));
		assertThat(registry.isValidRole("fostercare", "FOSTER_PARENT")).isTrue();
	}

	@Test
	void isValidRoleFalseForUnknownRole() {
		final var registry = new StakeholderRoleRegistryImpl(List.of(fostercare()));
		assertThat(registry.isValidRole("fostercare", "OTHER")).isFalse();
	}

	@Test
	void isValidRoleFalseForUnknownType() {
		final var registry = new StakeholderRoleRegistryImpl(List.of(fostercare()));
		assertThat(registry.isValidRole("unknown-type", "FOSTER_PARENT")).isFalse();
	}

	@Test
	void knownTypesReturnsRegisteredTypeSlugs() {
		final var registry = new StakeholderRoleRegistryImpl(List.of(fostercare(), adoption()));
		assertThat(registry.knownTypes()).containsExactlyInAnyOrder("fostercare", "adoption");
	}

	@Test
	void emptyRegistryHasNoTypes() {
		final var registry = new StakeholderRoleRegistryImpl(List.of());
		assertThat(registry.knownTypes()).isEmpty();
	}
}
