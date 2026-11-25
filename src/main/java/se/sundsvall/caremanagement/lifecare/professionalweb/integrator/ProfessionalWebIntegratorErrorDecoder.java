package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebErrors;
import se.sundsvall.dept44.configuration.feign.decoder.WSO2RetryResponseVerifier;

import static se.sundsvall.dept44.configuration.feign.decoder.AbstractErrorDecoder.MAX_ERROR_BODY_SIZE;

/**
 * Turns an error answer from lifecare-integrator's ProfessionalWeb resources into careM's problem, keeping the
 * integrator's status where careM passes it on and its detail, which carries Lifecare's own text. A rejected token is
 * retryable, so that the OAuth2 retryer evicts it and fetches a new one.
 */
class ProfessionalWebIntegratorErrorDecoder implements ErrorDecoder {

	private final WSO2RetryResponseVerifier retryResponseVerifier = new WSO2RetryResponseVerifier();

	@Override
	public Exception decode(final String methodKey, final Response response) {
		final var problem = ProfessionalWebErrors.fromIntegrator(response.status(), readBody(response));
		if (retryResponseVerifier.shouldReturnRetryableException(response)) {
			return new RetryableException(response.status(), retryResponseVerifier.getMessage(), response.request().httpMethod(), problem, (Long) null, response.request());
		}
		return problem;
	}

	private static byte[] readBody(final Response response) {
		if (response.body() == null) {
			return new byte[0];
		}
		try (var body = response.body().asInputStream()) {
			return body.readNBytes(MAX_ERROR_BODY_SIZE);
		} catch (final IOException _) {
			return new byte[0];
		}
	}
}
