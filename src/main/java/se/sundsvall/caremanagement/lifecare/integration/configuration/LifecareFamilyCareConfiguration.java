package se.sundsvall.caremanagement.lifecare.integration.configuration;

import feign.Logger;
import feign.RequestTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static java.util.Optional.ofNullable;

/**
 * Builds the {@link se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareClient} customizer. FamilyCare
 * authenticates with a {@code domain} + {@code key}, both required as query parameters; the spec also accepts the key
 * as an {@code X-API-Key} header, so we send both. The header is harmless where ignored and lets us drop the
 * query-string key once Tieto confirms header auth fleet-wide.
 *
 * <p>
 * The key is chosen per request: FamilyCare licences the {@code Users/*} directory to its own consumer, separate from
 * the person-based case APIs, so requests into {@code /Users} are authenticated with
 * {@link LifecareFamilyCareProperties#userKeyOrDefault()} and everything else with
 * {@link LifecareFamilyCareProperties#key()}. Where no separate user key is configured both resolve to the same key.
 *
 * <p>
 * Feign logging is forced to {@link feign.Logger.Level#NONE}, overriding the dept44 default of {@code FULL}.
 * FamilyCare reads carry the applicant's {@code personId} and the {@code key} secret as query parameters and return
 * income/calculation payloads as bodies; at any level above {@code NONE} Feign would log the request URL (personal
 * identity number + secret) and/or the bodies as soon as the client logger is raised to {@code DEBUG}. Pinning it to
 * {@code NONE} keeps that impossible regardless of the configured log level.
 */
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(LifecareFamilyCareProperties.class)
public class LifecareFamilyCareConfiguration {

	public static final String CLIENT_ID = "lifecare-familycare";

	/** The path segment marking the separately licensed FamilyCare user directory ({@code /apifc/v1/Users/*}). */
	static final String USERS_PATH_SEGMENT = "/Users";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final LifecareFamilyCareProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestInterceptor(template -> {
				final var key = keyFor(template.path(), properties);

				queryOnce(template, "domain", properties.domain());
				queryOnce(template, "key", key);
				template.header("X-API-Key", key);
			})
			.withCustomizer(builder -> builder.logLevel(Logger.Level.NONE))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.composeCustomizersToOne();
	}

	/**
	 * Add a query parameter unless the template already carries it. Feign re-applies the request interceptors to the
	 * <em>same</em> template on every retry attempt and {@link RequestTemplate#query(String, String...)} appends, so
	 * without this a retried request would go out with {@code domain} and {@code key} repeated once per attempt.
	 */
	private static void queryOnce(final RequestTemplate template, final String name, final String value) {
		if (!template.queries().containsKey(name)) {
			template.query(name, value);
		}
	}

	/** The licence key the given request path is authenticated with — see the class documentation. */
	static String keyFor(final String path, final LifecareFamilyCareProperties properties) {
		if (ofNullable(path).orElse("").contains(USERS_PATH_SEGMENT)) {
			return properties.userKeyOrDefault();
		}
		return properties.key();
	}
}
