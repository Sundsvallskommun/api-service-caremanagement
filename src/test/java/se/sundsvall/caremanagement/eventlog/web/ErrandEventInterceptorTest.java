package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandEventInterceptorTest {

	private static final String ERRAND_ID = UUID.randomUUID().toString();
	private static final String ROW_ID = UUID.randomUUID().toString();

	@Mock
	private ErrandEventService serviceMock;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@AfterEach
	void cleanup() {
		Identifier.remove();
		RequestId.reset();
	}

	private ErrandEventInterceptor interceptor() {
		return new ErrandEventInterceptor(serviceMock);
	}

	private ErrandEventEntity capture() {
		final var captor = ArgumentCaptor.forClass(ErrandEventEntity.class);
		verify(serviceMock).record(captor.capture());
		return captor.getValue();
	}

	@Test
	void recordsPlainErrandRead() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 200);

		interceptor().afterCompletion(request, response, new Object(), null);

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(entity.getSource()).isEqualTo("HTTP");
		assertThat(entity.getAction()).isEqualTo("READ");
		assertThat(entity.getTarget()).isEqualTo("errand");
		assertThat(entity.getDescription()).isEqualTo("READ errand");
		assertThat(entity.getHttpMethod()).isEqualTo("GET");
		assertThat(entity.getStatusCode()).isEqualTo(200);
		assertThat(entity.getActor()).isNull();
		assertThat(entity.getActorType()).isNull();
	}

	@Test
	void recordsSubResourceCreateWithActor() {
		Identifier.set(Identifier.parse("joe001doe; type=adAccount"));
		stub("POST", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/decisions", 201);

		interceptor().afterCompletion(request, response, new Object(), null);

		final var entity = capture();
		assertThat(entity.getAction()).isEqualTo("CREATE");
		assertThat(entity.getTarget()).isEqualTo("decisions");
		assertThat(entity.getActor()).isEqualTo("joe001doe");
		assertThat(entity.getActorType()).isEqualTo("adAccount");
		assertThat(entity.getStatusCode()).isEqualTo(201);
	}

	@Test
	void recordsFinancialAssistanceCalculationUpdateDroppingRowId() {
		stub("PATCH", "/2281/FINANCIAL_ASSISTANCE/errands/financial-assistance/" + ERRAND_ID + "/calculation/draft/incomes/" + ROW_ID, 200);

		interceptor().afterCompletion(request, response, new Object(), null);

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getAction()).isEqualTo("UPDATE");
		assertThat(entity.getTarget()).isEqualTo("financial-assistance/calculation/draft/incomes");
	}

	@Test
	void dropsNumericIdSegmentsFromTarget() {
		stub("PUT", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/attachments/12345", 200);

		interceptor().afterCompletion(request, response, new Object(), null);

		assertThat(capture().getTarget()).isEqualTo("attachments");
	}

	@Test
	void ignoresEmptyPathSegments() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "//notes", 200);

		interceptor().afterCompletion(request, response, new Object(), null);

		assertThat(capture().getTarget()).isEqualTo("notes");
	}

	@Test
	void recordsDelete() {
		stub("DELETE", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 204);

		interceptor().afterCompletion(request, response, new Object(), null);

		assertThat(capture().getAction()).isEqualTo("DELETE");
	}

	@Test
	void capturesRequestId() {
		RequestId.init("rid-123");
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 200);

		interceptor().afterCompletion(request, response, new Object(), null);

		assertThat(capture().getRequestId()).isEqualTo("rid-123");
	}

	@Test
	void skipsEligibilityRouteWithNoErrandId() {
		stubMethodAndUri("POST", "/2281/FINANCIAL_ASSISTANCE/errands/financial-assistance/eligibility");

		interceptor().afterCompletion(request, response, new Object(), null);

		verifyNoInteractions(serviceMock);
	}

	@Test
	void skipsErrandCollectionRoute() {
		stubMethodAndUri("GET", "/2281/FINANCIAL_ASSISTANCE/errands");

		interceptor().afterCompletion(request, response, new Object(), null);

		verifyNoInteractions(serviceMock);
	}

	@Test
	void skipsNonErrandRoute() {
		stubMethodAndUri("GET", "/2281/FINANCIAL_ASSISTANCE/metadata");

		interceptor().afterCompletion(request, response, new Object(), null);

		verifyNoInteractions(serviceMock);
	}

	@Test
	void skipsReadsOfTheEventLogItself() {
		stubMethodAndUri("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/events");

		interceptor().afterCompletion(request, response, new Object(), null);

		verifyNoInteractions(serviceMock);
	}

	@Test
	void skipsNonCrudMethod() {
		when(request.getMethod()).thenReturn("OPTIONS");

		interceptor().afterCompletion(request, response, new Object(), null);

		verify(serviceMock, never()).record(any());
	}

	@Test
	void swallowsExceptionsFromRecording() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 200);
		doThrow(new RuntimeException("db down")).when(serviceMock).record(any());

		assertThatNoException().isThrownBy(() -> interceptor().afterCompletion(request, response, new Object(), null));
	}

	private void stub(final String method, final String uri, final int status) {
		when(request.getMethod()).thenReturn(method);
		when(request.getRequestURI()).thenReturn(uri);
		when(response.getStatus()).thenReturn(status);
	}

	private void stubMethodAndUri(final String method, final String uri) {
		when(request.getMethod()).thenReturn(method);
		lenient().when(request.getRequestURI()).thenReturn(uri);
	}
}
