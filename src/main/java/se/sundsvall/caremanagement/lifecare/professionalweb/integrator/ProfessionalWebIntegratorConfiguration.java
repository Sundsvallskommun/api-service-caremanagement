package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import feign.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration;
import se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorProperties;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;

/**
 * The ProfessionalWeb client to lifecare-integrator: the same OAuth2 registration and timeouts as the FamilyCare
 * client, its own error decoder. Feign logging stays off, as the bodies carry personnummer.
 */
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(LifecareIntegratorProperties.class)
public class ProfessionalWebIntegratorConfiguration {

	@Bean
	FeignBuilderCustomizer professionalWebIntegratorFeignBuilderCustomizer(final LifecareIntegratorProperties properties,
		final ClientRegistrationRepository clientRegistrationRepository) {

		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProfessionalWebIntegratorErrorDecoder())
			.withCustomizer(builder -> builder.logLevel(Logger.Level.NONE))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(LifecareIntegratorConfiguration.CLIENT_ID))
			.composeCustomizersToOne();
	}
}
