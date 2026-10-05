package se.sundsvall.caremanagement.lifecare.integration;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.util.Map;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Builds the problem a Lifecare client really throws: an upstream response run through the client's own error decoder.
 * The 404 handling depends on how dept44's decoder renders the status, so the tests take the problem from the decoder
 * instead of writing the rendered text by hand — if the format ever changes, they fail here.
 */
public final class DecodedProblems {

	private DecodedProblems() {
		// utility class
	}

	/**
	 * The problem the decoder produces for an upstream answer.
	 *
	 * @param  decoder the error decoder the client is built with
	 * @param  status  the HTTP status of the answer
	 * @param  body    the response body, or {@code null} for none
	 * @return         the decoded problem
	 */
	public static ThrowableProblem decode(final ErrorDecoder decoder, final int status, final String body) {
		final var request = Request.create(Request.HttpMethod.GET, "http://upstream/person", Map.of(), null, UTF_8, new RequestTemplate());
		final var response = Response.builder()
			.status(status)
			.request(request)
			.headers(Map.of());
		if (body != null) {
			response.body(body, UTF_8);
		}
		return (ThrowableProblem) decoder.decode("Client#getPerson", response.build());
	}
}
