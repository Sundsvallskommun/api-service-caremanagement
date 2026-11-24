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
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

/**
 * Maps a ProfessionalWeb answer that is not a success onto the problem careM returns to its own caller.
 *
 * <p>
 * Most api2 failures are a status and an empty body; when one does carry a sentence it is passed on, because a
 * caseworker whose write was refused has to see why in Lifecare's words. Anything unrecognised becomes a 502: the call
 * careM received was fine, it is the system behind it that would not answer.
 * </p>
 */
public final class ProfessionalWebErrors {

	/** Lifecare's own status for understood but refused: a validation refusal the caseworker can fix. */
	static final int LIFECARE_REFUSED_STATUS = 461;

	/** The keys Lifecare puts its error sentence under. */
	private static final List<String> MESSAGE_KEYS = List.of("exceptionMessage", "ExceptionMessage", "message", "Message");

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final List<Integer> PASSED_ON_STATUSES = List.of(400, 403, 404, 422);

	private ProfessionalWebErrors() {}

	/**
	 * The problem for a failed response.
	 *
	 * @param  response the response
	 * @return          the problem to throw
	 */
	public static ThrowableProblem toProblem(final ProfessionalWebResponse response) {
		final var message = message(response);
		return switch (response.status()) {
			case 400 -> problem(BAD_REQUEST, message.orElse("Bad request to Lifecare"));
			// Not a session problem: Lifecare is saying this account may not read this.
			case 403 -> problem(FORBIDDEN, "The Lifecare account is not allowed to read this");
			case 404 -> problem(NOT_FOUND, "Not found in Lifecare");
			case LIFECARE_REFUSED_STATUS -> problem(UNPROCESSABLE_CONTENT, message.orElse("Lifecare godtog inte uppgifterna."));
			default -> problem(BAD_GATEWAY, message.orElse("Lifecare answered " + response.status()));
		};
	}

	/**
	 * The problem for an error answer from lifecare-integrator's ProfessionalWeb resources: 400, 403, 404 and 422 keep
	 * their status, anything else is a 502. The integrator's detail, Lifecare's own text when there is one, goes along.
	 *
	 * @param  status the integrator's status
	 * @param  body   the integrator's problem body, may be empty
	 * @return        the problem to throw
	 */
	public static ThrowableProblem fromIntegrator(final int status, final byte[] body) {
		final var detail = integratorDetail(body).orElse("lifecare-integrator answered " + status);
		if (PASSED_ON_STATUSES.contains(status)) {
			return problem(HttpStatus.valueOf(status), detail);
		}
		return problem(BAD_GATEWAY, detail);
	}

	/**
	 * Makes a call to lifecare-integrator. A problem from the integrator passes unchanged; anything else (a timeout, an
	 * integrator that cannot be reached, an open circuit) is a 502.
	 *
	 * @param  call the call
	 * @param  <T>  the answer's type
	 * @return      the answer
	 */
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
		if (body == null || body.length == 0) {
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

	/**
	 * Whether a problem is Lifecare turning a request down (400, or 422 for its own 461), as opposed to not answering.
	 *
	 * @param  problem the problem
	 * @return         true for a refusal
	 */
	public static boolean isRefusal(final ThrowableProblem problem) {
		return Optional.ofNullable(problem.getStatus())
			.map(status -> status.value() == BAD_REQUEST.value() || status.value() == UNPROCESSABLE_CONTENT.value())
			.orElse(false);
	}

	/**
	 * Lifecare's own sentence for a refusal, when its answer carries one.
	 *
	 * @param  response the response
	 * @return          the trimmed message, if any
	 */
	static Optional<String> message(final ProfessionalWebResponse response) {
		if (response.body().length == 0) {
			return Optional.empty();
		}
		try {
			final var node = JSON.readTree(response.body());
			return MESSAGE_KEYS.stream()
				.map(node::get)
				.filter(value -> value != null && value.isString() && !value.asString().isBlank())
				.map(value -> value.asString().trim())
				.findFirst();
		} catch (final JacksonException _) {
			return Optional.empty();
		}
	}

	private static ThrowableProblem problem(final HttpStatus status, final String detail) {
		return Problem.valueOf(status, detail);
	}
}
