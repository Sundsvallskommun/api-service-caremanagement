package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Maps errors from lifecare-integrator's ProfessionalWeb resources onto careM's problems. Lifecare's sentence in the
 * integrator's detail is passed on, because a caseworker whose write was refused has to see why in Lifecare's words.
 */
public final class ProfessionalWebErrors {

	/** The integrator's statuses careM passes on; any other is a 502. */
	private static final List<Integer> PASSED_ON_STATUSES = List.of(400, 403, 404, 422);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private ProfessionalWebErrors() {}

	public static ThrowableProblem fromIntegrator(final int status, final byte[] body) {
		final var detail = integratorDetail(body).orElse("lifecare-integrator answered " + status);
		if (PASSED_ON_STATUSES.contains(status)) {
			return Problem.valueOf(HttpStatus.valueOf(status), detail);
		}
		return Problem.valueOf(BAD_GATEWAY, detail);
	}

	/** A problem from the integrator passes unchanged; any other failure of the call is a 502. */
	public static <T> T call(final Supplier<T> call) {
		try {
			return call.get();
		} catch (final ThrowableProblem problem) {
			throw problem;
		} catch (final RuntimeException e) {
			throw Problem.valueOf(BAD_GATEWAY, "The call to lifecare-integrator failed (" + e.getClass().getSimpleName() + ")");
		}
	}

	/**
	 * Whether a failure is an answer from Lifecare that careM passes on (400, 403, 404, 422), as opposed to the
	 * integrator or Lifecare not answering. After a Create, only the latter leaves it uncertain whether it was written.
	 */
	public static boolean isPassedOn(final Throwable failure) {
		return failure instanceof final ThrowableProblem problem
			&& Optional.ofNullable(problem.getStatus()).map(status -> PASSED_ON_STATUSES.contains(status.value())).orElse(false);
	}

	private static Optional<String> integratorDetail(final byte[] body) {
		if (body.length == 0) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(JSON.readTree(body).get("detail"))
				.filter(value -> value.isString() && !value.asString().isBlank())
				.map(value -> value.asString().trim());
		} catch (final JacksonException _) {
			return Optional.empty();
		}
	}
}
