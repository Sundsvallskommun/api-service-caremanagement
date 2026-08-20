package se.sundsvall.caremanagement.lifecare.integration.configuration;

import feign.Feign;
import feign.RequestInterceptor;
import feign.Retryer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareClient;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the interceptor through a real Feign client rather than a hand-built {@code RequestTemplate}, so the
 * per-request key selection is verified against the path Feign actually produces for the client's own mappings — the
 * {@code Users/*} directory is licensed to a different consumer than the person-based case APIs and must go out with
 * the user key.
 */
@ExtendWith(MockitoExtension.class)
class LifecareFamilyCareKeySelectionTest {

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer feignBuilderCustomizerMock;

	@Mock
	private LifecareFamilyCareProperties propertiesMock;

	@Test
	void usersGoOutWithTheUserKeyAndPersonBasedReadsWithTheMainKey() {
		final var requestedUrl = new AtomicReference<String>();
		final var client = clientCapturing(requestedUrl, interceptor());

		assertThatThrownBy(() -> client.getUsers(100, null, null, null)).isInstanceOf(SentinelException.class);
		assertThat(requestedUrl.get()).contains("key=the-user-key", "X-API-Key");

		assertThatThrownBy(() -> client.getPerson("199001011234")).isInstanceOf(SentinelException.class);
		assertThat(requestedUrl.get()).contains("key=the-key").doesNotContain("the-user-key");
	}

	/** The interceptor the configuration builds, captured from the customizer. */
	private RequestInterceptor interceptor() {
		when(propertiesMock.domain()).thenReturn("the-domain");
		when(propertiesMock.key()).thenReturn("the-key");
		when(propertiesMock.userKeyOrDefault()).thenReturn("the-user-key");
		when(propertiesMock.connectTimeout()).thenReturn(1);
		when(propertiesMock.readTimeout()).thenReturn(2);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(feignBuilderCustomizerMock);

		try (MockedStatic<FeignMultiCustomizer> feignMultiCustomizerMock = Mockito.mockStatic(FeignMultiCustomizer.class)) {
			feignMultiCustomizerMock.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			new LifecareFamilyCareConfiguration().feignBuilderCustomizer(propertiesMock);

			final ArgumentCaptor<RequestInterceptor> captor = ArgumentCaptor.forClass(RequestInterceptor.class);
			verify(feignMultiCustomizerSpy).withRequestInterceptor(captor.capture());
			return captor.getValue();
		}
	}

	/** A FamilyCare client whose transport records the request URL and aborts before any decoding happens. */
	private static LifecareFamilyCareClient clientCapturing(final AtomicReference<String> requestedUrl, final RequestInterceptor interceptor) {
		return Feign.builder()
			.contract(new SpringMvcContract())
			.retryer(Retryer.NEVER_RETRY)
			.requestInterceptor(interceptor)
			.client((request, options) -> {
				requestedUrl.set(request.url() + " " + request.headers());
				throw new SentinelException();
			})
			.target(LifecareFamilyCareClient.class, "http://familycare.test/WESE.FC.Api.FC");
	}

	/**
	 * Marks "the request was built and handed to the transport" — the response itself is of no interest here.
	 * Unchecked so Feign propagates it as-is instead of wrapping it in a {@code RetryableException}.
	 */
	private static final class SentinelException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
