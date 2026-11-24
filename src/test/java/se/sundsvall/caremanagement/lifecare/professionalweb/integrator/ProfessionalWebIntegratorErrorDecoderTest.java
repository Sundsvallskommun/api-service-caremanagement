package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import feign.Request;
import feign.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfessionalWebIntegratorErrorDecoderTest {

	private final ProfessionalWebIntegratorErrorDecoder decoder = new ProfessionalWebIntegratorErrorDecoder();

	@ParameterizedTest
	@ValueSource(ints = {
		400, 403, 404, 422
	})
	void keepsTheIntegratorsStatusAndDetail(final int status) {
		final var problem = decode(status, "{\"title\":\"x\",\"status\":%d,\"detail\":\"Datum i framtiden\"}".formatted(status));

		assertThat(problem.getStatus().value()).isEqualTo(status);
		assertThat(problem.getDetail()).isEqualTo("Datum i framtiden");
	}

	@ParameterizedTest
	@ValueSource(ints = {
		401, 409, 500, 502, 503, 504
	})
	void anyOtherStatusIsABadGatewayWithTheIntegratorsDetail(final int status) {
		final var problem = decode(status, "{\"title\":\"Bad Gateway\",\"status\":%d,\"detail\":\"Lifecare svarade inte\"}".formatted(status));

		assertThat(problem.getStatus().value()).isEqualTo(502);
		assertThat(problem.getDetail()).isEqualTo("Lifecare svarade inte");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', nullValues = "NULL", value = {
		"422|{\"title\":\"Unprocessable Content\",\"status\":422}|422|lifecare-integrator answered 422",
		"404|{\"detail\":\"   \"}|404|lifecare-integrator answered 404",
		"400|not json|400|lifecare-integrator answered 400",
		"500|NULL|502|lifecare-integrator answered 500",
		"403|{\"detail\":42}|403|lifecare-integrator answered 403"
	})
	void aProblemWithoutADetailGetsAStandardTextWithTheStatus(final int status, final String body, final int expectedStatus, final String expectedDetail) {
		final var problem = decode(status, body);

		assertThat(problem.getStatus().value()).isEqualTo(expectedStatus);
		assertThat(problem.getDetail()).isEqualTo(expectedDetail);
	}

	@Test
	void theDetailIsTrimmed() {
		final var problem = decode(422, "{\"detail\":\"  Fel datum \"}");

		assertThat(problem.getDetail()).isEqualTo("Fel datum");
	}

	@Test
	void aBodyThatCannotBeReadGetsAStandardTextWithTheStatus() throws IOException {
		final var unreadable = mock(Response.Body.class);
		when(unreadable.asInputStream()).thenThrow(new IOException("socket gone"));

		final var problem = decode(responseBuilder(422).body(unreadable).build());

		assertThat(problem.getStatus().value()).isEqualTo(422);
		assertThat(problem.getDetail()).isEqualTo("lifecare-integrator answered 422");
	}

	private ThrowableProblem decode(final int status, final String body) {
		final var bytes = Optional.ofNullable(body).map(text -> text.getBytes(StandardCharsets.UTF_8)).orElse(null);
		return decode(responseBuilder(status).body(bytes).build());
	}

	private static Response.Builder responseBuilder(final int status) {
		return Response.builder()
			.status(status)
			.reason("reason")
			.request(Request.create(Request.HttpMethod.GET, "http://integrator/2281/professional-web/x", Map.of(), null, StandardCharsets.UTF_8, null))
			.headers(Map.of());
	}

	private ThrowableProblem decode(final Response response) {
		final var exception = decoder.decode("ProfessionalWebIntegratorClient#x()", response);
		assertThat(exception).isInstanceOf(ThrowableProblem.class);
		return (ThrowableProblem) exception;
	}
}
