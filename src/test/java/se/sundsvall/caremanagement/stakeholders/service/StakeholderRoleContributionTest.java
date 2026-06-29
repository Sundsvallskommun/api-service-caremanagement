package se.sundsvall.caremanagement.stakeholders.service;

import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StakeholderRoleContributionTest {

	@Test
	void accessors() {
		final var roles = Set.of(new RoleDefinition("APPLICANT", "Applicant", 1, true));
		final var contribution = new StakeholderRoleContribution("fostercare", roles);

		assertThat(contribution.typeSlug()).isEqualTo("fostercare");
		assertThat(contribution.roles()).containsExactlyElementsOf(roles);
	}

	@Test
	void rolesAreCopiedDefensively() {
		final var original = new java.util.HashSet<>(Set.of(new RoleDefinition("X", "X", 1, true)));
		final var contribution = new StakeholderRoleContribution("t", original);
		original.clear();
		assertThat(contribution.roles()).hasSize(1);
	}

	@Test
	void blankTypeSlugRejected() {
		assertThatThrownBy(() -> new StakeholderRoleContribution("  ", Set.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("typeSlug");
	}

	@Test
	void nullTypeSlugRejected() {
		assertThatThrownBy(() -> new StakeholderRoleContribution(null, Set.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("typeSlug");
	}
}
