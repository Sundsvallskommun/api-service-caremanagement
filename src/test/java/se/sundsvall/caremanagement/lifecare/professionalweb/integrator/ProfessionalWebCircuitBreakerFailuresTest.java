package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class ProfessionalWebCircuitBreakerFailuresTest {

	@Autowired
	private CircuitBreakerRegistry registry;

	private final ProfessionalWebCircuitBreakerFailures failures = new ProfessionalWebCircuitBreakerFailures();

	@ParameterizedTest
	@ValueSource(ints = {
		400, 403, 404, 422
	})
	void anAnswerFromLifecareIsNoFailure(final int status) {
		assertThat(failures.test(Problem.valueOf(HttpStatus.valueOf(status), "x"))).isFalse();
	}

	@Test
	void aBadGatewayOrAnythingElseIsAFailure() {
		assertThat(failures.test(Problem.valueOf(BAD_GATEWAY, "x"))).isTrue();
		assertThat(failures.test(new IllegalStateException("connection reset"))).isTrue();
	}

	@Test
	void theProfessionalWebBreakerUsesIt() {
		final var config = registry.circuitBreaker(ProfessionalWebIntegratorClient.CLIENT_ID).getCircuitBreakerConfig();

		assertThat(config.getRecordExceptionPredicate().test(Problem.valueOf(UNPROCESSABLE_CONTENT, "x"))).isFalse();
		assertThat(config.getRecordExceptionPredicate().test(Problem.valueOf(BAD_GATEWAY, "x"))).isTrue();
	}
}
