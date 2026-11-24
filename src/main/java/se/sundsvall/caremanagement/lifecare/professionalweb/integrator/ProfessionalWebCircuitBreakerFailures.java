package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.util.List;
import java.util.function.Predicate;
import se.sundsvall.dept44.problem.ThrowableProblem;

/**
 * What counts against the ProfessionalWeb client's circuit breaker: everything but an answer from Lifecare that careM
 * passes on (400, 403, 404, 422). A caseworker's refused write is not a failing integrator.
 */
public class ProfessionalWebCircuitBreakerFailures implements Predicate<Throwable> {

	private static final List<Integer> ANSWERS = List.of(400, 403, 404, 422);

	@Override
	public boolean test(final Throwable failure) {
		return !(failure instanceof final ThrowableProblem problem && problem.getStatus() != null && ANSWERS.contains(problem.getStatus().value()));
	}
}
