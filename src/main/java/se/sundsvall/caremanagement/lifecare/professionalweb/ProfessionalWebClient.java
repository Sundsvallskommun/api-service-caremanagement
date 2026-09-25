package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
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
 * Session escalation, as Lifecare's own client does it: when an answer says a session is needed, first bootstrap the
 * module and ask again, then sign in afresh and ask again, then give up with 502. Retrying a write is safe here,
 * because a session refusal comes before Lifecare acts on the request. A success is never retried, and nothing else
 * is retried at all.
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

	private final ProfessionalWebProperties properties;
	private final ProfessionalWebSession session;
	private final ProfessionalWebHttp http;
	private final JsonMapper json = JsonMapper.builder().build();

	public ProfessionalWebClient(final ProfessionalWebProperties properties, final ProfessionalWebSession session, final ProfessionalWebHttp http) {
		this.properties = properties;
		this.session = session;
		this.http = http;
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
		var response = send(method, path, params, body);

		if (ProfessionalWebHttp.needsSession(response)) {
			// First escalation: the module wants an artifact of its own. The ordinary case for a session that has not spoken
			// to this module yet.
			LOG.warn("Lifecare wants a session for {} ({}) - bootstrapping it", MODULE, response.describe());
			session.bootstrapModule(MODULE);
			response = send(method, path, params, body);
		}
		if (ProfessionalWebHttp.needsSession(response)) {
			// Second escalation: the session itself is gone. Lifecare sessions time out on idle.
			LOG.warn("Lifecare still refuses {} ({}) - signing in again", path, response.describe());
			session.reset();
			response = send(method, path, params, body);
		}
		if (ProfessionalWebHttp.needsSession(response)) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare would not accept a freshly established session (" + response.describe() + ")");
		}
		if (!response.isSuccess()) {
			LOG.info("Lifecare refused {} {} ({})", method, path, response.describe());
			throw ProfessionalWebErrors.toProblem(response);
		}
		return response;
	}

	private ProfessionalWebResponse send(final String method, final String path, final Map<String, String> params, final Object body) {
		final var headers = new LinkedHashMap<String, String>();
		headers.putAll(ProfessionalWebHttp.BROWSER_HEADERS);
		headers.putAll(ProfessionalWebHttp.AJAX_HEADERS);
		// Lifecare's own client sends these on every api2 call, and ASP.NET applications of this vintage are prone to
		// checking them.
		headers.put("Origin", origin());
		headers.put("Referer", properties.baseUrl() + "/WE.Flow.Html/");
		byte[] bytes = null;
		if (!GET.equals(method)) {
			headers.put("Content-Type", "application/json; charset=UTF-8");
			bytes = serialise(body);
		}
		headers.putAll(session.prepare());

		final var response = http.send(method, uri(path, params), headers, bytes);
		session.absorb(response);
		return response;
	}

	private URI uri(final String path, final Map<String, String> params) {
		final var base = properties.baseUrl() + "/" + MODULE + "/" + path.replaceAll("^/+", "");
		if (params == null || params.isEmpty()) {
			return URI.create(base);
		}
		final var query = params.entrySet().stream()
			.map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
			.collect(Collectors.joining("&"));
		return URI.create(base + "?" + query);
	}

	private String origin() {
		final var uri = URI.create(properties.baseUrl());
		return uri.getScheme() + "://" + uri.getRawAuthority();
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

	private static String encode(final String value) {
		if (value == null) {
			return "";
		}
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static boolean startsWith(final byte[] body, final byte[] prefix) {
		return body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
	}
}
