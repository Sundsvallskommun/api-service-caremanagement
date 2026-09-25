package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Transport for Lifecare ProfessionalWeb's api2 endpoints.
 *
 * <p>
 * Callers deal in JSON trees and never in cookies. Trees rather than typed objects on purpose: every write to
 * ProfessionalWeb is the object Lifecare handed out (a proposal, or the current record) with a few fields changed, and
 * a typed round trip would silently drop every field the type does not know about.
 * </p>
 *
 * <p>
 * How the call reaches Lifecare, session escalation included, is the {@link ProfessionalWebTransport}'s business:
 * directly
 * from inside the municipal network, or through api-service-lifecare-integrator from outside it. Nothing is retried
 * here.
 * </p>
 *
 * <p>
 * Nothing that crosses this client is logged beyond the path and the status: answers carry personnummer, and request
 * bodies carry whatever the caseworker wrote.
 * </p>
 */
@Component
public class ProfessionalWebClient {

	/** The one Lifecare module careM talks to. */
	public static final String MODULE = "WESE.FC.ProfessionalWeb";

	private static final Logger LOG = LoggerFactory.getLogger(ProfessionalWebClient.class);
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String DELETE = "DELETE";
	private static final byte[] PDF_SIGNATURE = "%PDF".getBytes(StandardCharsets.US_ASCII);

	private final ProfessionalWebTransport transport;
	private final JsonMapper json = JsonMapper.builder().build();

	public ProfessionalWebClient(final ProfessionalWebTransport transport) {
		this.transport = transport;
	}

	/**
	 * Reads from api2.
	 *
	 * @param  path   the path below the module, e.g. {@code api2/Calculation/GetCalculation}
	 * @param  params query parameters, in order
	 * @return        the answer, a missing node when Lifecare answered with an empty body
	 */
	public JsonNode get(final String path, final Map<String, String> params) {
		return toJson(exchange(GET, path, params, null));
	}

	/**
	 * Writes to (or asks a computation of) api2 with a JSON body.
	 *
	 * @param  path   the path below the module
	 * @param  params query parameters, in order
	 * @param  body   the request body, serialised as JSON
	 * @return        the answer, a missing node when Lifecare answered with an empty body
	 */
	public JsonNode post(final String path, final Map<String, String> params, final Object body) {
		return toJson(exchange(POST, path, params, body));
	}

	/**
	 * Removes something. Lifecare's own client sends the id in a JSON body rather than the URL.
	 *
	 * @param  path the path below the module
	 * @param  body the request body
	 * @return      the answer, a missing node when Lifecare answered with an empty body
	 */
	public JsonNode delete(final String path, final Object body) {
		return toJson(exchange(DELETE, path, Map.of(), body));
	}

	/**
	 * Reads a file Lifecare renders, such as a decision as PDF.
	 *
	 * @param  path   the path below the module
	 * @param  params query parameters, in order
	 * @return        the PDF bytes
	 */
	public byte[] getPdf(final String path, final Map<String, String> params) {
		final var body = exchange(GET, path, params, null).body();
		if (!startsWith(body, PDF_SIGNATURE)) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare did not answer with a PDF");
		}
		return body;
	}

	private ProfessionalWebResponse exchange(final String method, final String path, final Map<String, String> params, final Object body) {
		byte[] bytes = null;
		if (!GET.equals(method)) {
			bytes = serialise(body);
		}
		final var response = transport.exchange(method, path, params, bytes);
		if (!response.isSuccess()) {
			LOG.info("Lifecare refused {} {} ({})", method, path, response.describe());
			throw ProfessionalWebErrors.toProblem(response);
		}
		return response;
	}

	private byte[] serialise(final Object body) {
		if (body == null) {
			return new byte[0];
		}
		return json.writeValueAsBytes(body);
	}

	private JsonNode toJson(final ProfessionalWebResponse response) {
		if (response.body().length == 0) {
			return MissingNode.getInstance();
		}
		try {
			return json.readTree(response.body());
		} catch (final JacksonException _) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare answered with something that is not JSON (" + response.describe() + ")");
		}
	}

	private static boolean startsWith(final byte[] body, final byte[] prefix) {
		return body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
	}
}
