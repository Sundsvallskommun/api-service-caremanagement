package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The cookies of one Lifecare session.
 *
 * <p>
 * A Lifecare session is several cookies at once: the ones the sign-in flow issues ({@code ASP.NET_SessionId},
 * {@code LEGACY-TOKEN}, {@code IDP}) plus the configuration cookies the identity portal would read off the query
 * string on a browser's first visit ({@code metadomain}, {@code actor}, {@code idpmethod}, {@code federationprofile}).
 * </p>
 *
 * <p>
 * Every cookie in that flow is bound to the Lifecare host, so Domain and Path are ignored outright: everything held is
 * sent to that one host and nowhere else. Deletions are honoured, which is what keeps a stale session id from being
 * sent after Lifecare stopped recognising it. Thread safe: every method synchronises on the store.
 * </p>
 */
public class ProfessionalWebCookies {

	/**
	 * The cookie whose presence means a session is signed in, rather than merely started. Not {@code ASP.NET_SessionId}:
	 * that one is handed to anybody who loads a page. {@code LEGACY-TOKEN} is issued at the far end of the SAML round
	 * trip, and is also the value ProfessionalWeb expects echoed in the {@code X-LEGACY-TOKEN} header on a write.
	 */
	public static final String SIGNED_IN_COOKIE = "LEGACY-TOKEN";

	private final Map<String, String> cookies = new LinkedHashMap<>();

	/**
	 * Sets one cookie directly, for the configuration cookies no response issues.
	 *
	 * @param name  the cookie name
	 * @param value the cookie value
	 */
	public synchronized void set(final String name, final String value) {
		cookies.put(name, value);
	}

	/**
	 * Takes in the Set-Cookie headers of one response, honouring the ones that delete a cookie.
	 *
	 * @param setCookieHeaders the raw header values
	 */
	public synchronized void absorbSetCookie(final List<String> setCookieHeaders) {
		for (final var header : setCookieHeaders) {
			final var parts = header.split(";");
			final var pair = splitPair(parts[0]);
			if (pair.isEmpty()) {
				continue;
			}
			final var attributes = List.of(parts).subList(1, parts.length);
			if (isDeletion(attributes)) {
				cookies.remove(pair.get().getKey());
			} else {
				cookies.put(pair.get().getKey(), pair.get().getValue());
			}
		}
	}

	/**
	 * The Cookie request header for everything held.
	 *
	 * @return the header value, empty while nothing is held
	 */
	public synchronized String toHeader() {
		return cookies.entrySet().stream()
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.collect(Collectors.joining("; "));
	}

	public synchronized boolean has(final String name) {
		return cookies.containsKey(name);
	}

	public synchronized Optional<String> get(final String name) {
		return Optional.ofNullable(cookies.get(name));
	}

	/**
	 * The names held, without values: safe to log, and enough to compare one session with another.
	 *
	 * @return the cookie names
	 */
	public synchronized List<String> names() {
		return new ArrayList<>(cookies.keySet());
	}

	public synchronized void clear() {
		cookies.clear();
	}

	private static Optional<Map.Entry<String, String>> splitPair(final String pair) {
		final var separator = pair.indexOf('=');
		if (separator <= 0) {
			return Optional.empty();
		}
		return Optional.of(Map.entry(pair.substring(0, separator).trim(), pair.substring(separator + 1).trim()));
	}

	private static boolean isDeletion(final List<String> attributes) {
		return attributes.stream()
			.map(ProfessionalWebCookies::splitPair)
			.flatMap(Optional::stream)
			.anyMatch(attribute -> isExpired(attribute.getKey().toLowerCase(Locale.ROOT), attribute.getValue()));
	}

	private static boolean isExpired(final String name, final String value) {
		if ("max-age".equals(name)) {
			try {
				return Long.parseLong(value) <= 0;
			} catch (final NumberFormatException _) {
				return false;
			}
		}
		if ("expires".equals(name)) {
			try {
				// ASP.NET writes the date with dashes (01-Jan-1970), which RFC 1123 spells with spaces.
				return !ZonedDateTime.parse(value.replace('-', ' '), DateTimeFormatter.RFC_1123_DATE_TIME).isAfter(ZonedDateTime.now());
			} catch (final DateTimeParseException _) {
				return false;
			}
		}
		return false;
	}
}
