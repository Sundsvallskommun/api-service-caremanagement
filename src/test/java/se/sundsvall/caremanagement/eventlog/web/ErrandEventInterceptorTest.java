package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
	private HttpServletRequest requestMock;

	@Mock
	private HttpServletResponse responseMock;

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
		verify(serviceMock).recordEvent(captor.capture());
		return captor.getValue();
	}

	@Test
	void recordsPlainErrandRead() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 200);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(entity.getSource()).isEqualTo("HTTP");
		assertThat(entity.getAction()).isEqualTo("READ");
		assertThat(entity.getTarget()).isEqualTo("errand");
		assertThat(entity.getDescription()).isEqualTo("Öppnade ärendet");
		assertThat(entity.getHttpMethod()).isEqualTo("GET");
		assertThat(entity.getStatusCode()).isEqualTo(200);
		assertThat(entity.getActor()).isNull();
		assertThat(entity.getActorType()).isNull();
	}

	@Test
	void recordsSubResourceCreateWithActor() {
		Identifier.set(Identifier.parse("joe001doe; type=adAccount"));
		stub("POST", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/decisions", 201);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

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

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getAction()).isEqualTo("UPDATE");
		assertThat(entity.getTarget()).isEqualTo("financial-assistance/calculation/draft/incomes");
		assertThat(entity.getDescription()).isEqualTo("Uppdaterade inkomst i utkastberäkningen");
	}

	@Test
	void dropsNumericIdSegmentsFromTarget() {
		stub("PUT", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/attachments/12345", 200);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		assertThat(capture().getTarget()).isEqualTo("attachments");
	}

	@Test
	void ignoresEmptyPathSegments() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "//notes", 200);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		assertThat(capture().getTarget()).isEqualTo("notes");
	}

	@Test
	void recordsDelete() {
		stub("DELETE", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 204);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		assertThat(capture().getAction()).isEqualTo("DELETE");
	}

	@Test
	void capturesRequestId() {
		RequestId.init("rid-123");
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 200);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		assertThat(capture().getRequestId()).isEqualTo("rid-123");
	}

	@ParameterizedTest(name = "skips {0} {1}")
	@MethodSource
	void skipsNonRecordableRoute(final String method, final String uri) {
		stubMethodAndUri(method, uri);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		verifyNoInteractions(serviceMock);
	}

	static Stream<Arguments> skipsNonRecordableRoute() {
		return Stream.of(
			arguments("POST", "/2281/FINANCIAL_ASSISTANCE/errands/financial-assistance/eligibility"),
			arguments("GET", "/2281/FINANCIAL_ASSISTANCE/errands"),
			arguments("GET", "/2281/FINANCIAL_ASSISTANCE/metadata"),
			arguments("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/events"),
			arguments("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/messages/unread-count"),
			arguments("POST", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/messages/read"),
			arguments("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/notes/count"),
			arguments("GET", "/2281/FINANCIAL_ASSISTANCE/errands/financial-assistance/" + ERRAND_ID + "/warnings/count"));
	}

	@Test
	void stillRecordsRegularMessageRoutes() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID + "/messages", 200);

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		assertThat(capture().getTarget()).isEqualTo("messages");
	}

	@Test
	void skipsNonCrudMethod() {
		when(requestMock.getMethod()).thenReturn("OPTIONS");

		interceptor().afterCompletion(requestMock, responseMock, new Object(), null);

		verify(serviceMock, never()).recordEvent(any());
	}

	@Test
	void swallowsExceptionsFromRecording() {
		stub("GET", "/2281/FINANCIAL_ASSISTANCE/errands/" + ERRAND_ID, 200);
		doThrow(new RuntimeException("db down")).when(serviceMock).recordEvent(any());

		assertThatNoException().isThrownBy(() -> interceptor().afterCompletion(requestMock, responseMock, new Object(), null));
	}

	private void stub(final String method, final String uri, final int status) {
		when(requestMock.getMethod()).thenReturn(method);
		when(requestMock.getRequestURI()).thenReturn(uri);
		when(responseMock.getStatus()).thenReturn(status);
	}

	private void stubMethodAndUri(final String method, final String uri) {
		when(requestMock.getMethod()).thenReturn(method);
		lenient().when(requestMock.getRequestURI()).thenReturn(uri);
	}
}
