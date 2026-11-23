package se.sundsvall.caremanagement.lifecare;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * lifecare-integrator is the only way to Lifecare, so its url and OAuth2 registration are required: application.yml
 * carries no default for any of them, and an environment that does not set them cannot resolve them, which is what
 * stops the application from starting.
 */
class LifecareIntegratorRequiredSettingsTest {

	@ParameterizedTest
	@CsvSource({
		"integration.lifecare-integrator.url, INTEGRATION_LIFECARE_INTEGRATOR_URL",
		"spring.security.oauth2.client.registration.lifecare-integrator.client-id, INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_ID",
		"spring.security.oauth2.client.registration.lifecare-integrator.client-secret, INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_SECRET",
		"spring.security.oauth2.client.provider.lifecare-integrator.token-uri, INTEGRATION_LIFECARE_INTEGRATOR_TOKEN_URI"
	})
	void cannotBeResolvedWhenTheEnvironmentDoesNotSetIt(final String property, final String variable) throws IOException {
		final var environment = applicationYmlWith(Map.of());

		assertThatThrownBy(() -> environment.getProperty(property))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining(variable);
	}

	@ParameterizedTest
	@CsvSource({
		"integration.lifecare-integrator.url, INTEGRATION_LIFECARE_INTEGRATOR_URL",
		"spring.security.oauth2.client.registration.lifecare-integrator.client-id, INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_ID",
		"spring.security.oauth2.client.registration.lifecare-integrator.client-secret, INTEGRATION_LIFECARE_INTEGRATOR_CLIENT_SECRET",
		"spring.security.oauth2.client.provider.lifecare-integrator.token-uri, INTEGRATION_LIFECARE_INTEGRATOR_TOKEN_URI"
	})
	void isTakenFromTheEnvironment(final String property, final String variable) throws IOException {
		final var environment = applicationYmlWith(Map.of(variable, "from-the-environment"));

		assertThat(environment.getProperty(property)).isEqualTo("from-the-environment");
	}

	/** application.yml alone, with the given variables as the only environment. */
	private static StandardEnvironment applicationYmlWith(final Map<String, Object> variables) throws IOException {
		final var environment = new StandardEnvironment();
		final var sources = environment.getPropertySources();
		sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		sources.addLast(new MapPropertySource("variables", variables));
		new YamlPropertySourceLoader().load("application.yml", new ClassPathResource("application.yml")).forEach(sources::addLast);
		return environment;
	}
}
