package se.sundsvall.caremanagement.eneo.integration.configuration;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.eneo.integration.configuration.EneoProperties.Oauth2;

import static org.assertj.core.api.Assertions.assertThat;

class EneoPropertiesTest {

	@Test
	void accessors() {
		final var oauth2 = new Oauth2("http://token", "client", "secret", "client_credentials");
		final var properties = new EneoProperties("http://eneo", "api-key", oauth2, 5, 30);

		assertThat(properties.url()).isEqualTo("http://eneo");
		assertThat(properties.apiKey()).isEqualTo("api-key");
		assertThat(properties.oauth2()).isEqualTo(oauth2);
		assertThat(properties.oauth2().tokenUrl()).isEqualTo("http://token");
		assertThat(properties.oauth2().clientId()).isEqualTo("client");
		assertThat(properties.oauth2().clientSecret()).isEqualTo("secret");
		assertThat(properties.oauth2().authorizationGrantType()).isEqualTo("client_credentials");
		assertThat(properties.connectTimeoutInSeconds()).isEqualTo(5);
		assertThat(properties.readTimeoutInSeconds()).isEqualTo(30);
	}
}
