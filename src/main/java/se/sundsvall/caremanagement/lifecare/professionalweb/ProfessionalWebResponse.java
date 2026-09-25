package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
	 * A one-line description for a log line: status, type and where it was pointing. Carries no body, so it is safe to
	 * log anywhere.
	 *
	 * @return the description
	 */
	public String describe() {
		final var description = new StringBuilder("status ").append(status);
		final var type = contentType().split(";")[0];
		if (!type.isBlank()) {
			description.append(", type ").append(type);
		}
		location().ifPresent(location -> description.append(", redirects to ").append(location));
		return description.toString();
	}
}
