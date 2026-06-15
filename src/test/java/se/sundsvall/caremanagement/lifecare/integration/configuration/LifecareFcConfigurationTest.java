package se.sundsvall.caremanagement.lifecare.integration.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.lifecare.integration.configuration.LifecareFcConfiguration.CLIENT_ID;

@ExtendWith(MockitoExtension.class)
class LifecareFcConfigurationTest {

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private LifecareFcProperties propertiesMock;

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new LifecareFcConfiguration();

		when(propertiesMock.domain()).thenReturn("the-domain");
		when(propertiesMock.key()).thenReturn("the-key");
		when(propertiesMock.connectTimeout()).thenReturn(1);
		when(propertiesMock.readTimeout()).thenReturn(2);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			final var customizer = configuration.feignBuilderCustomizer(propertiesMock);

			final ArgumentCaptor<ProblemErrorDecoder> errorDecoderCaptor = ArgumentCaptor.forClass(ProblemErrorDecoder.class);
			final ArgumentCaptor<RequestInterceptor> interceptorCaptor = ArgumentCaptor.forClass(RequestInterceptor.class);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(feignMultiCustomizerSpy).withRequestInterceptor(interceptorCaptor.capture());
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(1, 2);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(feignBuilderCustomizerMock);

			// The interceptor must add domain + key as query parameters and the key as an X-API-Key header.
			final var template = new RequestTemplate();
			interceptorCaptor.getValue().apply(template);

			assertThat(template.queries().get("domain")).containsExactly("the-domain");
			assertThat(template.queries().get("key")).containsExactly("the-key");
			assertThat(template.headers().get("X-API-Key")).containsExactly("the-key");
		}
	}
}
