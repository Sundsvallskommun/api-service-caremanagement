package se.sundsvall.caremanagement.eneo.integration.configuration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import se.sundsvall.caremanagement.eneo.integration.EneoClient;
import se.sundsvall.caremanagement.eneo.integration.EneoIntegration;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

/**
 * Builds the single {@link EneoClient} from {@code integration.eneo.*}: the base url + {@code api-key} header (Eneo app
 * auth) and a programmatic OAuth2 client-credentials registration (api-gateway auth). Mirrors the proven
 * api-service-ai-flow setup, simplified to one client.
 */
@Configuration
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(EneoProperties.class)
public class EneoConfiguration {

	private final ApplicationContext applicationContext;
	private final EneoProperties properties;

	EneoConfiguration(final ApplicationContext applicationContext, final EneoProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	FeignHttpMessageConverters feignHttpMessageConverters(
		final ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers,
		final ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers) {
		return new FeignHttpMessageConverters(customizers, cloudCustomizers);
	}

	@Bean
	EneoClient eneoClient() {
		return new FeignClientBuilder(applicationContext)
			.forType(EneoClient.class, EneoIntegration.CLIENT_ID)
			.customize(FeignMultiCustomizer.create()
				.withErrorDecoder(new ProblemErrorDecoder(EneoIntegration.CLIENT_ID))
				// Eneo app auth — the gateway forwards this to the eneo application.
				.withRequestInterceptor(request -> request.header("api-key", properties.apiKey()))
				// API-gateway auth — OAuth2 client-credentials bearer.
				.withRetryableOAuth2InterceptorForClientRegistration(ClientRegistration
					.withRegistrationId(EneoIntegration.CLIENT_ID)
					.tokenUri(properties.oauth2().tokenUrl())
					.clientId(properties.oauth2().clientId())
					.clientSecret(properties.oauth2().clientSecret())
					.authorizationGrantType(new AuthorizationGrantType(properties.oauth2().authorizationGrantType()))
					.build())
				.withRequestTimeoutsInSeconds(properties.connectTimeoutInSeconds(), properties.readTimeoutInSeconds())
				.composeCustomizersToOne())
			.url(properties.url())
			.build();
	}
}
