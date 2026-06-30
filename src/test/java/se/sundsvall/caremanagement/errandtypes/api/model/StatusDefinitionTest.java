package se.sundsvall.caremanagement.errandtypes.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusDefinitionTest {

	@Test
	void accessors() {
		final var def = new StatusDefinition("UNDER_REVIEW", "Under utredning");

		assertThat(def.code()).isEqualTo("UNDER_REVIEW");
		assertThat(def.displayName()).isEqualTo("Under utredning");
	}

	@Test
	void nullDisplayNameAllowed() {
		final var def = new StatusDefinition("RECEIVED", null);

		assertThat(def.code()).isEqualTo("RECEIVED");
		assertThat(def.displayName()).isNull();
	}

	@Test
	void blankCodeRejected() {
		assertThatThrownBy(() -> new StatusDefinition("  ", "x"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("code");
	}

	@Test
	void nullCodeRejected() {
		assertThatThrownBy(() -> new StatusDefinition(null, "x"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("code");
	}
}
