package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One answer from Lifecare, whatever its status. Carries no logic beyond reading its own headers; deciding what a
 * status means is the transport's job.
 *
 * @param status  the HTTP status (or Lifecare's own 360/461)
 * @param headers the response headers
 * @param body    the raw body, never null
 * @param uri     the URI that was requested, so a relative form action or redirect can be resolved
 */
public record ProfessionalWebResponse(int status, HttpHeaders headers, byte[] body, URI uri) {

	public ProfessionalWebResponse {
		body = Optional.ofNullable(body).orElse(new byte[0]);
	}

	public boolean isSuccess() {
		return status >= 200 && status < 300;
	}

	public Optional<String> location() {
		return headers.firstValue("Location");
	}

	public String contentType() {
		return headers.firstValue("Content-Type").orElse("");
	}

	public List<String> setCookies() {
		return headers.allValues("Set-Cookie");
	}

	public String bodyAsString() {
		return new String(body, StandardCharsets.UTF_8);
	}

	/**
	 * A one-line description for a log line: status, type and where it was pointing. Carries no body and no query
	 * string — a redirect to the login page carries the original request in its ReturnUrl, personnummer included — so
	 * it is safe to log anywhere.
	 *
	 * @return the description
	 */
	public String describe() {
		final var description = new StringBuilder("status ").append(status);
		final var type = contentType().split(";")[0];
		if (!type.isBlank()) {
			description.append(", type ").append(type);
		}
		location().ifPresent(location -> description.append(", redirects to ").append(withoutQuery(location)));
		return description.toString();
	}

	/** The part of a URL before its query string. */
	static String withoutQuery(final String url) {
		final var cut = url.indexOf('?');
		if (cut < 0) {
			return url;
		}
		return url.substring(0, cut);
	}

	/**
	 * Value equality on {@code body} (byte content, not array identity). The other components use their own
	 * {@code equals}.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final ProfessionalWebResponse other)) {
			return false;
		}
		return (status == other.status)
			&& Objects.equals(headers, other.headers)
			&& Arrays.equals(body, other.body)
			&& Objects.equals(uri, other.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, headers, Arrays.hashCode(body), uri);
	}

	/**
	 * Safe for a log line through a careless {@code toString()}: the body by its length, the headers by their names (a
	 * Set-Cookie value is the session credential) and the URI without its query (which can carry a personnummer).
	 */
	@Override
	public String toString() {
		return "ProfessionalWebResponse[status=%d, headers=%s, body=%d bytes, uri=%s]".formatted(status, headers.map().keySet(), body.length,
			Optional.ofNullable(uri).map(URI::toString).map(ProfessionalWebResponse::withoutQuery).orElse(null));
	}
}
