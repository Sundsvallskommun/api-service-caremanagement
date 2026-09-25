package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebResponse;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebTransport;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Reaches Lifecare ProfessionalWeb through api-service-lifecare-integrator, for a careM that runs outside the
 * municipal network. The integrator holds the integration account's session and does the session escalation; this
 * side only packs the call and unpacks Lifecare's answer.
 */
@Component
@ConditionalOnProperty(name = ProfessionalWebTransport.PROVIDER_PROPERTY, havingValue = "integrator")
class IntegratorProfessionalWebTransport implements ProfessionalWebTransport {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	/** The integrator's path takes a municipality; there is one Lifecare, Sundsvall's. */
	private static final String MUNICIPALITY_ID = "2281";
	private static final URI INTEGRATOR = URI.create("lifecare-integrator:professional-web");

	private final ProfessionalWebIntegratorClient client;

	IntegratorProfessionalWebTransport(final ProfessionalWebIntegratorClient client) {
		this.client = client;
	}

	@Override
	public ProfessionalWebResponse exchange(final String method, final String path, final Map<String, String> params, final byte[] body) {
		final var jsonBody = Optional.ofNullable(body)
			.filter(bytes -> bytes.length > 0)
			.map(JSON::readTree)
			.orElse(null);
		final var answer = Optional.ofNullable(client.exchange(MUNICIPALITY_ID, new ProfessionalWebExchangeRequest(method, path, params, jsonBody)))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "lifecare-integrator answered without Lifecare's answer"));

		final var headers = Optional.ofNullable(answer.contentType())
			.map(type -> HttpHeaders.of(Map.of("Content-Type", List.of(type)), (name, value) -> true))
			.orElseGet(() -> HttpHeaders.of(Map.of(), (name, value) -> true));
		final var bytes = Optional.ofNullable(answer.body()).map(Base64.getDecoder()::decode).orElse(null);
		return new ProfessionalWebResponse(answer.status(), headers, bytes, INTEGRATOR);
	}
}
