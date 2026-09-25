package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.security.Truststore;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * The raw HTTP layer every ProfessionalWeb call goes through, the sign-in hops at the identity provider included.
 *
 * <p>
 * Built on the JDK client rather than Feign, deliberately and against the dept44 default: Lifecare answers a dead
 * session with a status of its own (360) and with redirects that must be detected rather than followed, rotates its
 * cookies on ordinary calls, and walks sign-in through HTML forms. None of that fits a declarative client, so statuses
 * are read here rather than thrown, and redirects are never followed automatically.
 * </p>
 *
 * <p>
 * Trust comes from the dept44 truststore, where Lifecare's internal CA is installed. No body is ever logged here:
 * everything that crosses this connection may hold personal data or credentials.
 * </p>
 */
@Component
public class ProfessionalWebHttp {

	/**
	 * Lifecare is an application that has only ever been spoken to by browsers. Presenting the browser the flow was
	 * captured from removes a variable that would otherwise have to be ruled out later.
	 */
	static final Map<String, String> BROWSER_HEADERS = Map.of(
		"User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36",
		"Accept-Language", "sv-SE,sv;q=0.9,en-US;q=0.8,en;q=0.7");

	/**
	 * What makes api2 answer an automated caller the way it answers Lifecare's own web client. Without
	 * X-Requested-With a dead session is answered with the login page as HTML; ajax-no-cross-domain-redirect turns that
	 * into the bare 360 a caller can act on. For data calls only: sent while following the bootstrap chain, they would
	 * make Lifecare signal instead of redirect and the session would never be established.
	 */
	static final Map<String, String> AJAX_HEADERS = Map.of(
		"Accept", "application/json, text/javascript, */*; q=0.01",
		"X-Requested-With", "XMLHttpRequest",
		"ajax-no-cross-domain-redirect", "true");

	static final Map<String, String> NAVIGATION_HEADERS = Map.of(
		"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

	/** Lifecare's own answer for this call needs a session. Not an HTTP status, a number its client agreed on. */
	static final int SESSION_REQUIRED_STATUS = 360;

	private static final String IDENTITY_PORTAL_PATH = "IdentityPortalWeb";
	private static final int MAX_REDIRECT_HOPS = 10;

	private final HttpClient client;
	private final Duration readTimeout;

	// Two constructors, so Spring has to be told which one it injects; the other is for tests.
	@Autowired
	public ProfessionalWebHttp(final ProfessionalWebProperties properties, final Truststore truststore) {
		this(HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			// Lifecare runs on IIS, which the fleet has pinned to HTTP/1.1 elsewhere for good reason.
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(properties.connectTimeout()))
			.sslContext(truststore.getSSLContext())
			.build(), Duration.ofSeconds(properties.readTimeout()));
	}

	ProfessionalWebHttp(final HttpClient client, final Duration readTimeout) {
		this.client = client;
		this.readTimeout = readTimeout;
	}

	/**
	 * Sends one request and returns whatever came back, whatever the status.
	 *
	 * @param  method  the HTTP method
	 * @param  uri     the absolute URI
	 * @param  headers the request headers
	 * @param  body    the body, or null for none
	 * @return         the response
	 */
	public ProfessionalWebResponse send(final String method, final URI uri, final Map<String, String> headers, final byte[] body) {
		final var builder = HttpRequest.newBuilder(uri).timeout(readTimeout);
		headers.forEach(builder::header);
		if (body == null) {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
		}

		try {
			final var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
			return new ProfessionalWebResponse(response.statusCode(), response.headers(), response.body(), uri);
		} catch (final IOException e) {
			// The exception type is the whole diagnosis (a refused connection says something else than a timeout), and it
			// holds nothing about the person being looked up.
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare could not be reached (" + e.getClass().getSimpleName() + ")");
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
			throw Problem.valueOf(BAD_GATEWAY, "Interrupted while calling Lifecare");
		}
	}

	/**
	 * Walks a redirect chain by hand, carrying the cookies from each hop into the next.
	 *
	 * <p>
	 * Followed manually because the cookies are the point: the bootstrap only works if what the identity portal sets on
	 * one hop is presented on the next. Passing form fields submits a form as the first hop; everything after it is a
	 * GET, which is what a browser does with a 302 answering a POST.
	 * </p>
	 *
	 * @param  startUrl   where the chain starts
	 * @param  cookies    the session cookies, updated along the way
	 * @param  formFields form fields to post as the first hop, or null for a plain navigation
	 * @return            the first response in the chain that is not a redirect
	 */
	public ProfessionalWebResponse followRedirects(final String startUrl, final ProfessionalWebCookies cookies, final Map<String, String> formFields) {
		var uri = URI.create(startUrl);
		var body = encodeForm(formFields);

		for (var hop = 0; hop < MAX_REDIRECT_HOPS; hop++) {
			final var headers = new LinkedHashMap<String, String>();
			headers.putAll(BROWSER_HEADERS);
			headers.putAll(NAVIGATION_HEADERS);
			final var cookieHeader = cookies.toHeader();
			if (!cookieHeader.isEmpty()) {
				headers.put("Cookie", cookieHeader);
			}
			var method = "GET";
			if (body != null) {
				method = "POST";
				headers.put("Content-Type", "application/x-www-form-urlencoded");
			}

			final var response = send(method, uri, headers, body);
			body = null;
			cookies.absorbSetCookie(response.setCookies());

			final var location = response.location();
			if (location.isEmpty()) {
				return response;
			}
			uri = uri.resolve(location.get());
		}

		throw Problem.valueOf(BAD_GATEWAY, "Lifecare kept redirecting while a session was being established");
	}

	/**
	 * Whether a response means the session is gone and must be established again.
	 *
	 * <p>
	 * Lifecare says it three ways depending on the endpoint: the 360 signal, a redirect back to the identity portal, and
	 * the login page delivered as HTML where JSON was expected. A 401 joins them because an expired ASP.NET session
	 * surfaces that way too. A 403 deliberately does not: that is Lifecare saying the account may not see this, and a
	 * new session would only hide a real permission problem behind a second failure.
	 * </p>
	 *
	 * @param  response the response
	 * @return          true when a new session is needed
	 */
	public static boolean needsSession(final ProfessionalWebResponse response) {
		if (response.status() == SESSION_REQUIRED_STATUS || response.status() == 401) {
			return true;
		}
		if (response.location().filter(location -> location.contains(IDENTITY_PORTAL_PATH)).isPresent()) {
			return true;
		}
		return response.contentType().contains("text/html");
	}

	static byte[] encodeForm(final Map<String, String> fields) {
		if (fields == null) {
			return null;
		}
		return fields.entrySet().stream()
			.map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
			.collect(Collectors.joining("&"))
			.getBytes(StandardCharsets.UTF_8);
	}
}
