package se.sundsvall.caremanagement.configuration;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Forces dept44's {@code se.sundsvall.dept44.payload} logger to OFF, suppressing Zalando Logbook request/response
 * logging. Caremanagement speaks to upstreams that carry personnummer and income data in the request itself — the
 * lifecare-fc reads put {@code personId=<pnr>} and the FC {@code key} in the query string, and the financial-aid
 * (SSBTEK) reads put {@code personalNumber} there — so the logged request line alone leaks sensitive data. Mirrors the
 * silencer in api-service-financial-aid.
 *
 * <p>
 * Registered via {@code META-INF/spring.factories} so it outranks dept44's
 * {@code classpath:/config/application.properties}; {@code application.yml} overrides and
 * {@code spring.autoconfigure.exclude} both lose that precedence fight.
 */
public class PayloadLoggerSilencer implements EnvironmentPostProcessor {

	private static final String PROPERTY_SOURCE_NAME = "caremanagementPayloadLoggerOverride";
	private static final String LOGGER_LEVEL_KEY = "logging.level.se.sundsvall.dept44.payload";

	@Override
	public void postProcessEnvironment(final ConfigurableEnvironment environment, final @NonNull SpringApplication application) {
		environment.getPropertySources().addFirst(
			new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(LOGGER_LEVEL_KEY, "OFF")));
	}
}
