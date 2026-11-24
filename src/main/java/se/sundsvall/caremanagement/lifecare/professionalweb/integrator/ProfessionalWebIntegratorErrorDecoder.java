package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebErrors;

/**
 * Turns an error answer from lifecare-integrator's ProfessionalWeb resources into careM's problem, keeping the
 * integrator's status where careM passes it on and its detail, which carries Lifecare's own text.
 */
class ProfessionalWebIntegratorErrorDecoder implements ErrorDecoder {

	@Override
	public Exception decode(final String methodKey, final Response response) {
		return ProfessionalWebErrors.fromIntegrator(response.status(), readBody(response));
	}

	private static byte[] readBody(final Response response) {
		if (response.body() == null) {
			return new byte[0];
		}
		try (var body = response.body().asInputStream()) {
			return body.readAllBytes();
		} catch (final IOException _) {
			return new byte[0];
		}
	}
}
