package se.sundsvall.caremanagement.operaton.integration.configuration;

import java.util.List;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Import(FeignConfiguration.class)
public class OperatonConfiguration {

	public static final String CLIENT_ID = "operaton";

	/**
	 * The engine answers {@code 404} when no process instance waits for a correlated message, and the correlation callers
	 * tell that from a failing engine, so it is kept as {@code 404} instead of the {@code 502} every other answer becomes.
	 */
	private static final List<Integer> BYPASS_RESPONSE_CODES = List.of(NOT_FOUND.value());

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(OperatonProperties operatonProperties, ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, BYPASS_RESPONSE_CODES))
			.withRequestTimeoutsInSeconds(operatonProperties.connectTimeout(), operatonProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}
}
