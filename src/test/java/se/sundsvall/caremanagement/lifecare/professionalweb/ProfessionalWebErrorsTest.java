package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

class ProfessionalWebErrorsTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"400|{\"exceptionMessage\":\" Fel datum \"}|400|Fel datum",
		"400||400|Bad request to Lifecare",
		"403|{\"Message\":\"secret\"}|403|The Lifecare account is not allowed to read this",
		"404||404|Not found in Lifecare",
		"461|{\"Message\":\"Tiden ligger i framtiden\"}|422|Tiden ligger i framtiden",
		"461|not json|422|Lifecare godtog inte uppgifterna.",
		"500|{\"message\":\"   \"}|502|Lifecare answered 500",
		"500|{\"ExceptionMessage\":\"Boom\"}|502|Boom"
	})
	void toProblem(final int status, final String body, final int expectedStatus, final String expectedDetail) {
		final var problem = ProfessionalWebErrors.toProblem(response(status, body));

		assertThat(problem.getStatus().value()).isEqualTo(expectedStatus);
		assertThat(problem.getDetail()).isEqualTo(expectedDetail);
	}

	@Test
	void isRefusal() {
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(BAD_REQUEST, "x"))).isTrue();
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(UNPROCESSABLE_CONTENT, "x"))).isTrue();
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(BAD_GATEWAY, "x"))).isFalse();
	}

	private static ProfessionalWebResponse response(final int status, final String body) {
		final var bytes = Optional.ofNullable(body).map(text -> text.getBytes(StandardCharsets.UTF_8)).orElse(null);
		return new ProfessionalWebResponse(status, HttpHeaders.of(Map.of(), (a, b) -> true), bytes, URI.create("https://lifecare.example"));
	}
}
