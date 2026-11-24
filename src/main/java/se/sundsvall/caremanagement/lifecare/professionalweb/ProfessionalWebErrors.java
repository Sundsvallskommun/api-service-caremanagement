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
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

/**
 * Maps errors from lifecare-integrator's ProfessionalWeb resources onto careM's problems. Lifecare's sentence in the
 * integrator's detail is passed on, because a caseworker whose write was refused has to see why in Lifecare's words.
 */
public final class ProfessionalWebErrors {

	/** The integrator's statuses careM passes on; any other is a 502. */
	public static final List<Integer> PASSED_ON_STATUSES = List.of(400, 403, 404, 422);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private ProfessionalWebErrors() {}

	public static ThrowableProblem fromIntegrator(final int status, final byte[] body) {
		final var detail = integratorDetail(body).orElse("lifecare-integrator answered " + status);
		if (PASSED_ON_STATUSES.contains(status)) {
			return problem(HttpStatus.valueOf(status), detail);
		}
		return problem(BAD_GATEWAY, detail);
	}

	/** A problem from the integrator passes unchanged; a timeout, an unreachable integrator or an open circuit is a 502. */
	public static <T> T call(final Supplier<T> call) {
		try {
			return call.get();
		} catch (final ThrowableProblem problem) {
			throw problem;
		} catch (final RuntimeException e) {
			throw problem(BAD_GATEWAY, "lifecare-integrator could not be reached (" + e.getClass().getSimpleName() + ")");
		}
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

	/** Whether a problem is Lifecare turning a request down (400 or 422), as opposed to not answering. */
	public static boolean isRefusal(final ThrowableProblem problem) {
		return Optional.ofNullable(problem.getStatus())
			.map(status -> status.value() == BAD_REQUEST.value() || status.value() == UNPROCESSABLE_CONTENT.value())
			.orElse(false);
	}

	private static ThrowableProblem problem(final HttpStatus status, final String detail) {
		return Problem.valueOf(status, detail);
	}
}
