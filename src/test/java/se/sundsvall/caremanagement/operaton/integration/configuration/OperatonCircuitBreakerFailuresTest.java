package se.sundsvall.caremanagement.operaton.integration.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class OperatonCircuitBreakerFailuresTest {

	@Autowired
	private CircuitBreakerRegistry registry;

	@Test
	void theEnginesNotFoundIsNoFailure() {
		final var failures = new OperatonCircuitBreakerFailures();

		assertThat(failures.test(Problem.valueOf(NOT_FOUND, "No process instance is waiting for message 'PaymentDecisionReceived'"))).isFalse();
		assertThat(failures.test(Problem.valueOf(BAD_GATEWAY, "engine down"))).isTrue();
		assertThat(failures.test(new IllegalStateException("connection reset"))).isTrue();
	}

	@Test
	void theOperatonBreakerUsesItWithTheDefaultSettings() {
		final var config = registry.circuitBreaker(OperatonConfiguration.CLIENT_ID).getCircuitBreakerConfig();

		assertThat(config.getRecordExceptionPredicate().test(Problem.valueOf(NOT_FOUND, "No process instance is waiting"))).isFalse();
		assertThat(config.getRecordExceptionPredicate().test(Problem.valueOf(BAD_GATEWAY, "engine down"))).isTrue();
		// dept44's defaults still apply to the instance.
		assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
		assertThat(config.getSlidingWindowSize()).isEqualTo(10);
	}
}
