package se.sundsvall.caremanagement.lifecare.integration.integrator;

import feign.Response;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

/**
 * The dept44 {@link ProblemErrorDecoder} with the error body buffered first.
 *
 * <p>
 * dept44's {@code AbstractErrorDecoder} reads the response body twice, and an OkHttp body is not repeatable, so the
 * second read comes back empty and the upstream explanation is replaced by {@code title=Unknown error}. The
 * lifecare-integrator answers in RFC 9457 and says exactly why Lifecare refused — {@code {"detail": "lifecare-fc
 * error: {detail=Saknar norm för angiven hushållsstorlek, …}"}} — and every word of it was being dropped here.
 *
 * <p>
 * That cost real time: each failure in the create_actualisation and commit_normberakning chain had to be diagnosed by
 * fetching the integrator's own log from ocp201, because the message that reached the caseworker's errand and the
 * process incident said only "Unknown error". The integrator carries the same class against FamilyCare for the same
 * reason; this is that fix one layer out, and both can go once dept44 reads the body once.
 */
public class BufferingProblemErrorDecoder extends ProblemErrorDecoder {

	private static final Logger LOG = LoggerFactory.getLogger(BufferingProblemErrorDecoder.class);

	public BufferingProblemErrorDecoder(final String integrationName) {
		super(integrationName);
	}

	@Override
	public Exception decode(final String methodKey, final Response response) {
		return super.decode(methodKey, withRepeatableBody(response));
	}

	/**
	 * The same response, with its body replaced by an equivalent repeatable one. Returns the response untouched when
	 * there is nothing to buffer, or when reading it fails — a failed read must not mask the error being decoded.
	 */
	static Response withRepeatableBody(final Response response) {
		final var body = response.body();
		if (body == null || body.isRepeatable()) {
			return response;
		}

		try (body) {
			return response.toBuilder()
				.body(body.asInputStream().readAllBytes())
				.build();
		} catch (final IOException e) {
			LOG.warn("Could not buffer the error response body: {}", e.getMessage());
			return response;
		}
	}
}
