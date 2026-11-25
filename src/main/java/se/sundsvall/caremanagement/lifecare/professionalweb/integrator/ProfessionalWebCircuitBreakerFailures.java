package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.util.function.Predicate;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebErrors;

/**
 * What counts against the ProfessionalWeb client's circuit breaker: everything but an answer from Lifecare that careM
 * passes on (400, 403, 404, 422). A caseworker's refused write is not a failing integrator.
 */
public class ProfessionalWebCircuitBreakerFailures implements Predicate<Throwable> {

	@Override
	public boolean test(final Throwable failure) {
		return !ProfessionalWebErrors.isPassedOn(failure);
	}
}
