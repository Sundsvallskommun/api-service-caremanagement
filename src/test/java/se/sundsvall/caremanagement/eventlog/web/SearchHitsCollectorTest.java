package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.FindErrandsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchHitsCollectorTest {

	private final SearchHitsCollector collector = new SearchHitsCollector();

	@Mock
	private HttpServletRequest servletRequestMock;

	@Mock
	private ServerHttpResponse responseMock;

	private ServerHttpRequest request() {
		return new ServletServerHttpRequest(servletRequestMock);
	}

	private static FindErrandsResponse hits(final String... ids) {
		final var response = new FindErrandsResponse();
		response.setErrands(Arrays.stream(ids).map(id -> Errand.create().withId(id)).toList());
		return response;
	}

	@Test
	void supportsOnlyASearchResponseBody() throws NoSuchMethodException {
		final var findErrandsReturnType = new MethodParameter(ReturnTypes.class.getDeclaredMethod("findErrands"), -1);
		final var otherReturnType = new MethodParameter(ReturnTypes.class.getDeclaredMethod("readErrand"), -1);

		assertThat(collector.supports(findErrandsReturnType, null)).isTrue();
		assertThat(collector.supports(otherReturnType, null)).isFalse();
	}

	@Test
	void supportsASearchResponseWrappedInAResponseEntity() throws NoSuchMethodException {
		// The shape ErrandResource.findErrands really has: Spring hands the advice the handler's own return type.
		final var wrappedSearch = new MethodParameter(ReturnTypes.class.getDeclaredMethod("findErrandsWrapped"), -1);
		final var wrappedOther = new MethodParameter(ReturnTypes.class.getDeclaredMethod("readErrandWrapped"), -1);

		assertThat(collector.supports(wrappedSearch, null)).isTrue();
		assertThat(collector.supports(wrappedOther, null)).isFalse();
	}

	/** Stand-ins for a controller method's return type, so {@code supports()} can be exercised without a real resource. */
	@SuppressWarnings("unused")
	private static final class ReturnTypes {
		static FindErrandsResponse findErrands() {
			return null;
		}

		static Errand readErrand() {
			return null;
		}

		static ResponseEntity<FindErrandsResponse> findErrandsWrapped() {
			return null;
		}

		static ResponseEntity<Errand> readErrandWrapped() {
			return null;
		}
	}

	@Test
	void stashesTheReturnedErrandIds() {
		final var body = hits("a", "b");

		final var returned = collector.beforeBodyWrite(body, null, null, null, request(), responseMock);

		final var captor = ArgumentCaptor.forClass(Object.class);
		verify(servletRequestMock).setAttribute(org.mockito.ArgumentMatchers.eq(SearchHitsCollector.SEARCH_HITS_ATTRIBUTE), captor.capture());
		assertThat(captor.getValue()).isEqualTo(List.of("a", "b"));
		// The advice must hand the body straight back — it observes, it does not rewrite.
		assertThat(returned).isSameAs(body);
	}

	@Test
	void skipsNullsAndDuplicateIds() {
		final var body = new FindErrandsResponse();
		body.setErrands(Arrays.asList(Errand.create().withId("a"), null, Errand.create().withId("a"), Errand.create()));

		collector.beforeBodyWrite(body, null, null, null, request(), responseMock);

		final var captor = ArgumentCaptor.forClass(Object.class);
		verify(servletRequestMock).setAttribute(org.mockito.ArgumentMatchers.eq(SearchHitsCollector.SEARCH_HITS_ATTRIBUTE), captor.capture());
		assertThat(captor.getValue()).isEqualTo(List.of("a"));
	}

	@Test
	void ignoresAnyOtherResponseBody() {
		collector.beforeBodyWrite("not a search", null, null, null, request(), responseMock);

		verifyNoInteractions(servletRequestMock);
	}

	@Test
	void ignoresANonServletRequest() {
		final var body = hits("a");

		final var returned = collector.beforeBodyWrite(body, null, null, null, mock(ServerHttpRequest.class), responseMock);

		assertThat(returned).isSameAs(body);
		verifyNoInteractions(servletRequestMock);
	}

	@Test
	void handlesAnEmptyOrAbsentHitList() {
		collector.beforeBodyWrite(new FindErrandsResponse(), null, null, null, request(), responseMock);

		verify(servletRequestMock).setAttribute(SearchHitsCollector.SEARCH_HITS_ATTRIBUTE, List.of());
	}
}
