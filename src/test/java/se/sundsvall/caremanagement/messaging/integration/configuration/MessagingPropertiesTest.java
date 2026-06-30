package se.sundsvall.caremanagement.messaging.integration.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingPropertiesTest {

	@Test
	void accessors() {
		final var properties = new MessagingProperties(5, 30);

		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
