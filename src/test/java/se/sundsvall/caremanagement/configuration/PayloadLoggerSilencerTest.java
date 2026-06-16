package se.sundsvall.caremanagement.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadLoggerSilencerTest {

	private static final String LOGGER_LEVEL_KEY = "logging.level.se.sundsvall.dept44.payload";
	private static final String PROPERTY_SOURCE_NAME = "caremanagementPayloadLoggerOverride";

	private final PayloadLoggerSilencer silencer = new PayloadLoggerSilencer();

	@Test
	void forcesPayloadLoggerOff() {
		final var environment = new MockEnvironment();

		silencer.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getProperty(LOGGER_LEVEL_KEY)).isEqualTo("OFF");
	}

	@Test
	void overrideIsAddedFirstSoItWins() {
		final var environment = new MockEnvironment();
		environment.setProperty(LOGGER_LEVEL_KEY, "TRACE");

		silencer.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getPropertySources()).first()
			.extracting(PropertySource::getName)
			.isEqualTo(PROPERTY_SOURCE_NAME);
		assertThat(environment.getProperty(LOGGER_LEVEL_KEY)).isEqualTo("OFF");
	}
}
