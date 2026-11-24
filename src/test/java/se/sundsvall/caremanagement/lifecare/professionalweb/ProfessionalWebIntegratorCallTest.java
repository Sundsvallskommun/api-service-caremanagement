package se.sundsvall.caremanagement.lifecare.professionalweb;

import feign.Request;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

class ProfessionalWebIntegratorCallTest {

	@Test
	void returnsTheAnswer() {
		assertThat(ProfessionalWebErrors.call(() -> "svar")).isEqualTo("svar");
	}

	@Test
	void aTimeoutIsABadGateway() {
		final var timeout = new RetryableException(-1, "Read timed out", Request.HttpMethod.POST, (Long) null,
			Request.create(Request.HttpMethod.POST, "http://integrator", Map.of(), null, StandardCharsets.UTF_8, null));

		assertThatThrownBy(() -> ProfessionalWebErrors.call(() -> {
			throw timeout;
		})).isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
			assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY);
			assertThat(problem.getDetail()).isEqualTo("lifecare-integrator could not be reached (RetryableException)");
		});
	}

	@Test
	void anOpenCircuitIsABadGateway() {
		final var open = CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("x"));

		assertThatThrownBy(() -> ProfessionalWebErrors.call(() -> {
			throw open;
		})).isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
			assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY);
			assertThat(problem.getDetail()).isEqualTo("lifecare-integrator could not be reached (CallNotPermittedException)");
		});
	}

	@Test
	void theIntegratorsProblemPassesUnchanged() {
		final var refusal = Problem.valueOf(UNPROCESSABLE_CONTENT, "Datum i framtiden");

		assertThatThrownBy(() -> ProfessionalWebErrors.call(() -> {
			throw refusal;
		})).isSameAs(refusal);
	}

	@ParameterizedTest
	@ValueSource(ints = {
		400, 422
	})
	void aRefusalIsOneFromLifecare(final int status) {
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(org.springframework.http.HttpStatus.valueOf(status), "x"))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = {
		403, 404, 502
	})
	void anythingElseIsNoRefusal(final int status) {
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(org.springframework.http.HttpStatus.valueOf(status), "x"))).isFalse();
	}

	@Test
	void anUnreachableIntegratorIsNoRefusal() {
		assertThatThrownBy(() -> ProfessionalWebErrors.call(() -> {
			throw new IllegalStateException("connection reset");
		})).isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(ProfessionalWebErrors.isRefusal(problem)).isFalse());
	}
}
