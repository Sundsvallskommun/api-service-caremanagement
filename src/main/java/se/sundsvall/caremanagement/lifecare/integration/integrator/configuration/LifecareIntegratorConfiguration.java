package se.sundsvall.caremanagement.lifecare.integration.integrator.configuration;

import feign.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.caremanagement.lifecare.integration.integrator.BufferingProblemErrorDecoder;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;

/**
 * Builds the customizer for the lifecare-integrator client. Unlike the direct FamilyCare client, which authenticates
 * with a {@code domain} + {@code key} pair in the query string, the integrator is an ordinary dept44 service behind the
 * API gateway and takes OAuth2 client credentials — so there is no secret in the URL here.
 *
 * <p>
 * Feign logging is pinned to {@link feign.Logger.Level#NONE}, overriding the dept44 default of {@code FULL}. The
 * reads carry a {@code partyId} and return calculation, decision and payment payloads; raising the client logger to
 * {@code DEBUG} at any other level would put those in the log. Pinning it here keeps that impossible regardless of the
 * configured log level, exactly as the direct FamilyCare client does.
 *
 * <p>
 * Note the {@code partyId} rather than a personal identity number: routing through the integrator means careM stops
 * handing a personnummer to the integration layer at all for these calls, which is a small privacy gain over the direct
 * route and not merely a change of address.
 */
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(LifecareIntegratorProperties.class)
public class LifecareIntegratorConfiguration {

	public static final String CLIENT_ID = "lifecare-integrator";

	@Bean
	FeignBuilderCustomizer lifecareIntegratorFeignBuilderCustomizer(final LifecareIntegratorProperties properties,
		final ClientRegistrationRepository clientRegistrationRepository) {

		return FeignMultiCustomizer.create()
			.withErrorDecoder(new BufferingProblemErrorDecoder(CLIENT_ID))
			.withCustomizer(builder -> builder.logLevel(Logger.Level.NONE))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
