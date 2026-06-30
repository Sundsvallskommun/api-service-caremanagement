package se.sundsvall.caremanagement.citizen.integration.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenPropertiesTest {

	@Test
	void accessors() {
		final var properties = new CitizenProperties(5, 30);

		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
