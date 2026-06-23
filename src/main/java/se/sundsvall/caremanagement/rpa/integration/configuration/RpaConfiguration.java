package se.sundsvall.caremanagement.rpa.integration.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;
import se.sundsvall.dept44.configuration.feign.interceptor.OAuth2RequestInterceptor;
import se.sundsvall.dept44.configuration.feign.retryer.ActionRetryer;

import static java.util.Collections.emptySet;

/**
 * Builds the {@link se.sundsvall.caremanagement.rpa.integration.RpaClient} customizer. UiPath Orchestrator is reached
 * with OAuth2 client-credentials against the {@code rpa} registration; on a 401 the token is dropped and the request
 * retried once.
 */
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(RpaProperties.class)
public class RpaConfiguration {

	public static final String CLIENT_ID = "rpa";

	@Bean
	FeignBuilderCustomizer rpaFeignBuilderCustomizer(final ClientRegistrationRepository clientRepository, final RpaProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withCustomizer(builder -> {
				final var oAuth2RequestInterceptor = new OAuth2RequestInterceptor(clientRepository.findByRegistrationId(CLIENT_ID), emptySet());
				builder.requestInterceptor(oAuth2RequestInterceptor);
				builder.retryer(new ActionRetryer(oAuth2RequestInterceptor::removeToken, 1));
			})
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.composeCustomizersToOne();
	}
}
