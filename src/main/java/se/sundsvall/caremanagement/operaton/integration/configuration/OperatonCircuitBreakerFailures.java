package se.sundsvall.caremanagement.operaton.integration.configuration;

import java.util.function.Predicate;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * What counts against the Operaton client's circuit breaker: every failure except the engine's {@code 404}. The engine
 * answers {@code 404} when no process instance waits for a correlated message, an answer about one errand that the
 * callers handle (see {@link OperatonConfiguration}); counted as a failure, a message retried hourly for an errand
 * without a process kept the breaker open and the service's health RESTRICTED.
 */
public class OperatonCircuitBreakerFailures implements Predicate<Throwable> {

	@Override
	public boolean test(final Throwable failure) {
		return !(failure instanceof final ThrowableProblem problem && NOT_FOUND.equals(problem.getStatus()));
	}
}
