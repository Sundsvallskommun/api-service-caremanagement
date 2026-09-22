package se.sundsvall.caremanagement.lifecare.integration.integrator.configuration;

import feign.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration.CLIENT_ID;

@ExtendWith(MockitoExtension.class)
class LifecareIntegratorConfigurationTest {

	@Mock
	private ClientRegistrationRepository clientRegistrationRepositoryMock;

	@Mock
	private ClientRegistration clientRegistrationMock;

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private LifecareIntegratorProperties propertiesMock;

	@Test
	void feignBuilderCustomizerIsComposedFromTheExpectedParts() {
		final var configuration = new LifecareIntegratorConfiguration();

		when(clientRegistrationRepositoryMock.findByRegistrationId(any())).thenReturn(clientRegistrationMock);
		when(propertiesMock.connectTimeout()).thenReturn(1);
		when(propertiesMock.readTimeout()).thenReturn(2);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			final var customizer = configuration.lifecareIntegratorFeignBuilderCustomizer(propertiesMock, clientRegistrationRepositoryMock);

			final ArgumentCaptor<ProblemErrorDecoder> errorDecoderCaptor = ArgumentCaptor.forClass(ProblemErrorDecoder.class);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(clientRegistrationRepositoryMock).findByRegistrationId(CLIENT_ID);
			verify(feignMultiCustomizerSpy).withRetryableOAuth2InterceptorForClientRegistration(same(clientRegistrationMock));
			verify(propertiesMock).connectTimeout();
			verify(propertiesMock).readTimeout();
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(1, 2);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);
		}
	}

	/**
	 * Feign logging must stay at {@link Logger.Level#NONE}: the reads carry a partyId and return calculation, decision
	 * and payment payloads, so anything above NONE puts personal data in the log as soon as the client logger is raised
	 * to DEBUG. Asserting the customizer is registered keeps a well-meaning "let's see the traffic" change honest.
	 */
	@Test
	void feignLoggingIsPinnedToNone() {
		final var configuration = new LifecareIntegratorConfiguration();

		when(clientRegistrationRepositoryMock.findByRegistrationId(any())).thenReturn(clientRegistrationMock);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			configuration.lifecareIntegratorFeignBuilderCustomizer(propertiesMock, clientRegistrationRepositoryMock);

			// FeignMultiCustomizer routes several of its own builder methods through withCustomizer, so apply them all
			// and assert that one of them pins the level.
			final ArgumentCaptor<FeignBuilderCustomizer> customizerCaptor = ArgumentCaptor.forClass(FeignBuilderCustomizer.class);
			verify(feignMultiCustomizerSpy, atLeastOnce()).withCustomizer(customizerCaptor.capture());

			// The OAuth2 customizer among them needs a fully built ClientRegistration to run, which this test has no
			// reason to construct — only the log-level one is under test, so the others are allowed to fail.
			final var builderMock = Mockito.mock(feign.Feign.Builder.class);
			customizerCaptor.getAllValues().forEach(customizer -> {
				try {
					customizer.customize(builderMock);
				} catch (final RuntimeException ignored) {
					// not the customizer under test
				}
			});
			verify(builderMock).logLevel(Logger.Level.NONE);
		}
	}
}
