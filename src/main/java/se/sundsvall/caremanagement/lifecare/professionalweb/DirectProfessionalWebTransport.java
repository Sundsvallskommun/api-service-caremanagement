package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Talks to Lifecare directly: one api2 call through the integration account's session, with Lifecare's own session
 * escalation.
 *
 * <p>
 * When an answer says a session is needed: first bootstrap the module and ask again, then sign in afresh and ask
 * again, then give up with 502. Only works from inside the municipal network. Retrying a write is safe here, because a
 * session refusal comes before Lifecare acts on
 * the request. Every other answer, success or refusal, is handed back as it is; interpreting it is the caller's job.
 * </p>
 *
 * <p>
 * Nothing that crosses this class is logged beyond the path and the status: answers carry personnummer, and request
 * bodies carry whatever the caseworker wrote.
 * </p>
 */
@Component
@ConditionalOnProperty(name = ProfessionalWebTransport.PROVIDER_PROPERTY, havingValue = "direct", matchIfMissing = true)
public class DirectProfessionalWebTransport implements ProfessionalWebTransport {

	private static final String MODULE = ProfessionalWebClient.MODULE;
	private static final String POST = "POST";
	private static final Logger LOG = LoggerFactory.getLogger(DirectProfessionalWebTransport.class);

	private final ProfessionalWebProperties properties;
	private final ProfessionalWebSession session;
	private final ProfessionalWebHttp http;

	public DirectProfessionalWebTransport(final ProfessionalWebProperties properties, final ProfessionalWebSession session, final ProfessionalWebHttp http) {
		this.properties = properties;
		this.session = session;
		this.http = http;
	}

	/**
	 * Sends one call and returns Lifecare's final answer, whatever its status.
	 *
	 * @param  method the HTTP method
	 * @param  path   the path below the module, e.g. {@code api2/Calculation/GetCalculation}
	 * @param  params query parameters, in order
	 * @param  body   the JSON body, or null for none
	 * @return        the answer
	 */
	@Override
	public ProfessionalWebResponse exchange(final String method, final String path, final Map<String, String> params, final byte[] body) {
		return withSession(method, path, () -> send(method, path, params, body));
	}

	@Override
	public ProfessionalWebResponse submitForm(final String path, final Map<String, String> params, final List<ProfessionalWebFormField> fields) {
		return withSession(POST, path, () -> sendForm(path, params, fields));
	}

	/** Sends, and when the answer says a session is needed, establishes one and sends again. */
	private ProfessionalWebResponse withSession(final String method, final String path, final Supplier<Sent> sender) {
		var sent = sender.get();

		if (ProfessionalWebHttp.needsSession(sent.response())) {
			LOG.warn("Lifecare wants a session for {} ({}) - bootstrapping it", MODULE, sent.response().describe());
			session.bootstrapModule(MODULE);
			sent = sender.get();
		}
		if (ProfessionalWebHttp.needsSession(sent.response())) {
			LOG.warn("Lifecare still refuses {} ({}) - signing in again", path, sent.response().describe());
			// Only the session this request used: a request that raced ahead may already have established a new one.
			session.reset(sent.session());
			sent = sender.get();
			if (ProfessionalWebHttp.needsSession(sent.response())) {
				// A fresh session's first module call asks for the module's artifact, like the first call ever did.
				session.bootstrapModule(MODULE);
				sent = sender.get();
			}
		}
		if (ProfessionalWebHttp.needsSession(sent.response())) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare would not accept a freshly established session (" + sent.response().describe() + ")");
		}
		if (!sent.response().isSuccess()) {
			LOG.info("Lifecare answered {} {} with {}", method, path, sent.response().describe());
		}
		return sent.response();
	}

	/** One answer, and the session it was sent on. */
	private record Sent(ProfessionalWebResponse response, Instant session) {}

	private Sent send(final String method, final String path, final Map<String, String> params, final byte[] body) {
		final var headers = new LinkedHashMap<String, String>();
		headers.putAll(ProfessionalWebHttp.BROWSER_HEADERS);
		headers.putAll(ProfessionalWebHttp.AJAX_HEADERS);
		headers.put("Origin", origin());
		headers.put("Referer", properties.baseUrl() + "/WE.Flow.Html/");
		byte[] bytes = null;
		if (!"GET".equals(method)) {
			headers.put("Content-Type", "application/json; charset=UTF-8");
			bytes = body;
			if (bytes == null) {
				bytes = new byte[0];
			}
		}
		headers.putAll(session.prepare());
		final var sessionUsed = session.established();

		final var response = http.send(method, uri(path, params), headers, bytes);
		session.absorb(response);
		return new Sent(response, sessionUsed);
	}

	/** A form posted back as the browser posts it; the fields and the token are never logged. */
	private Sent sendForm(final String path, final Map<String, String> params, final List<ProfessionalWebFormField> fields) {
		final var uri = uri(path, params);
		final var headers = new LinkedHashMap<String, String>();
		headers.putAll(ProfessionalWebHttp.BROWSER_HEADERS);
		headers.putAll(ProfessionalWebHttp.NAVIGATION_HEADERS);
		headers.put("Origin", origin());
		headers.put("Referer", uri.toString());
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.putAll(session.prepare());
		final var sessionUsed = session.established();

		final var response = http.send(POST, uri, headers, ProfessionalWebHttp.encodeFields(fields));
		session.absorb(response);
		return new Sent(response, sessionUsed);
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

	private static String encode(final String value) {
		if (value == null) {
			return "";
		}
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
