package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class RequireIdentifierInterceptorTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Test
	void passesWhenIdentifierValid() {
		when(request.getMethod()).thenReturn("POST");
		when(request.getHeader(Identifier.HEADER_NAME)).thenReturn("joe001doe; type=adAccount");

		assertThat(new RequireIdentifierInterceptor(true).preHandle(request, response, new Object())).isTrue();
	}

	@Test
	void rejectsWhenIdentifierMissing() {
		when(request.getMethod()).thenReturn("GET");
		when(request.getHeader(Identifier.HEADER_NAME)).thenReturn(null);

		assertThatThrownBy(() -> new RequireIdentifierInterceptor(true).preHandle(request, response, new Object()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void rejectsWhenIdentifierMalformed() {
		when(request.getMethod()).thenReturn("POST");
		when(request.getHeader(Identifier.HEADER_NAME)).thenReturn("not-a-valid-identifier");

		assertThatThrownBy(() -> new RequireIdentifierInterceptor(true).preHandle(request, response, new Object()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void allowsPreflightOptionsWithoutIdentifier() {
		when(request.getMethod()).thenReturn("OPTIONS");

		assertThat(new RequireIdentifierInterceptor(true).preHandle(request, response, new Object())).isTrue();
	}

	@Test
	void passesWhenEnforcementDisabled() {
		lenient().when(request.getMethod()).thenReturn("GET");

		assertThat(new RequireIdentifierInterceptor(false).preHandle(request, response, new Object())).isTrue();
	}
}
