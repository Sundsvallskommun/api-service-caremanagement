package se.sundsvall.caremanagement.lifecare.integration.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareFcPropertiesTest {

	@Test
	void accessors() {
		final var properties = new LifecareFcProperties("http://fc", "the-domain", "the-key", 5, 30);

		assertThat(properties.url()).isEqualTo("http://fc");
		assertThat(properties.domain()).isEqualTo("the-domain");
		assertThat(properties.key()).isEqualTo("the-key");
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
