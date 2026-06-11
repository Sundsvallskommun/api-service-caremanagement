package se.sundsvall.caremanagement.templating.integration.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatingPropertiesTest {

	@Test
	void accessors() {
		final var properties = new TemplatingProperties(5, 30);

		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
