package se.sundsvall.caremanagement.stakeholders.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleDefinitionTest {

	@Test
	void testAccessors() {
		final var def = new RoleDefinition("FOSTER_PARENT", "Familjehemsförälder", 2, true);

		assertThat(def.code()).isEqualTo("FOSTER_PARENT");
		assertThat(def.displayName()).isEqualTo("Familjehemsförälder");
		assertThat(def.maxOccurrences()).isEqualTo(2);
		assertThat(def.required()).isTrue();
	}

	@Test
	void testUnboundedOccurrencesAllowed() {
		final var def = new RoleDefinition("OBSERVER", "Observer", 0, false);
		assertThat(def.maxOccurrences()).isZero();
		assertThat(def.required()).isFalse();
	}

	@Test
	void testBlankCodeRejected() {
		assertThatThrownBy(() -> new RoleDefinition("  ", "x", 1, true))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("code");
	}

	@Test
	void testNullCodeRejected() {
		assertThatThrownBy(() -> new RoleDefinition(null, "x", 1, true))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("code");
	}
}
