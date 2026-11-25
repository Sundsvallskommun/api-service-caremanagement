package se.sundsvall.caremanagement.citizen.integration.configuration;

import feign.Logger;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

/**
 * Builds the {@link se.sundsvall.caremanagement.citizen.integration.CitizenClient} customizer.
 *
 * <p>
 * Feign logging is forced to {@link feign.Logger.Level#NONE}, overriding the dept44 default of {@code FULL}. Both
 * lookup methods carry the citizen's {@code personNumber} directly in the request URL, and at {@code FULL} the
 * response body (a personnummer) would also be logged once the client logger is raised to {@code DEBUG}. Pinning it
 * to {@code NONE} keeps that impossible regardless of log level, matching FinancialAidConfiguration and
 * LifecareIntegratorConfiguration.
 * </p>
 */
@Import(FeignConfiguration.class)
public class CitizenConfiguration {

	public static final String CLIENT_ID = "citizen";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final CitizenProperties citizenProperties, final ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withCustomizer(builder -> builder.logLevel(Logger.Level.NONE))
			.withRequestTimeoutsInSeconds(citizenProperties.connectTimeout(), citizenProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
