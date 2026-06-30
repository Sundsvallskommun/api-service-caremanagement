package se.sundsvall.caremanagement.financialaid.integration.configuration;

import feign.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

/**
 * Builds the {@link se.sundsvall.caremanagement.financialaid.integration.FinancialAidClient} customizer. financial-aid
 * (the SSBTEK proxy) is a dept44 service behind the API gateway, so it authenticates with OAuth2 client-credentials —
 * the same pattern as the other gateway integrations.
 *
 * <p>
 * Feign logging is forced to {@link feign.Logger.Level#NONE}, overriding the dept44 default of {@code FULL}. The basis
 * request carries the applicant's {@code personalNumber} as a query parameter and the response is the SSBTEK income
 * basis; at any level above {@code NONE} Feign would log the request URL (personnummer) and/or the income body once the
 * client logger is raised to {@code DEBUG}. Pinning it to {@code NONE} keeps that impossible regardless of log level.
 */
@Import(FeignConfiguration.class)
@EnableConfigurationProperties(FinancialAidProperties.class)
public class FinancialAidConfiguration {

	public static final String CLIENT_ID = "financial-aid";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final FinancialAidProperties properties, final ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withCustomizer(builder -> builder.logLevel(Logger.Level.NONE))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
