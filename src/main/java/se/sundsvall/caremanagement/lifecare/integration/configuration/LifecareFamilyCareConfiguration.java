package se.sundsvall.caremanagement.lifecare.integration.configuration;

import feign.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

/**
 * Builds the {@link se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareClient} customizer. FamilyCare
 * authenticates with a {@code domain} + {@code key}, both required as query parameters; the spec also accepts the key
 * as an {@code X-API-Key} header, so we send both. The header is harmless where ignored and lets us drop the
 * query-string key once Tieto confirms header auth fleet-wide.
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

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final LifecareFamilyCareProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestInterceptor(template -> {
				template.query("domain", properties.domain());
				template.query("key", properties.key());
				template.header("X-API-Key", properties.key());
			})
			.withCustomizer(builder -> builder.logLevel(Logger.Level.NONE))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.composeCustomizersToOne();
	}
}
