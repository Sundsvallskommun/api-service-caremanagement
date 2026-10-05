package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebCookies.SIGNED_IN_COOKIE;

/**
 * Keeps one Lifecare session alive and hands out the headers that prove it.
 *
 * <p>
 * One session for the whole process, shared by every caller: a session costs a sign-in and a bootstrap per module, so
 * one per request would multiply both for nothing. Sign-in and bootstrap are single-flight (callers arriving together
 * wait for the one in progress) and failures are never cached, so one bad minute cannot poison every call after it.
 * </p>
 *
 * <p>
 * The session lives in memory only. Signing in over HTTP takes a few round trips, so a restart simply signs in again;
 * persisting a live session would put a credential at rest for no real gain. A session past its TTL is replaced before
 * it is used, and the transport replaces it reactively if Lifecare drops it mid-flight.
 * </p>
 *
 * <p>
 * The module bootstrap is on demand: Lifecare's own client calls an endpoint and only when the module answers that it
 * needs a session does it fetch a Heartbeat artifact and ask again. The transport asks for it the same way.
 * </p>
 */
@Component
public class ProfessionalWebSession {

	private static final Logger LOG = LoggerFactory.getLogger(ProfessionalWebSession.class);

	private final ProfessionalWebProperties properties;
	private final ProfessionalWebSignIn signIn;
	private final ProfessionalWebHttp http;
	private final Clock clock;
	private final ProfessionalWebCookies cookies = new ProfessionalWebCookies();
	private final Set<String> bootstrappedModules = ConcurrentHashMap.newKeySet();
	private final Object signInLock = new Object();

	private volatile Instant establishedAt;

	ProfessionalWebSession(final ProfessionalWebProperties properties, final ProfessionalWebSignIn signIn, final ProfessionalWebHttp http, final Clock clock) {
		this.properties = properties;
		this.signIn = signIn;
		this.http = http;
		this.clock = clock;
	}

	/**
	 * Makes sure there is a session and returns the headers that carry it: the cookies, and the X-LEGACY-TOKEN
	 * ProfessionalWeb double-submit checks against its cookie on a write (a mismatch answers 500). Reads never need the
	 * header, but sending it always keeps the two paths identical.
	 *
	 * @return the session headers
	 */
	public Map<String, String> prepare() {
		if (!properties.isConfigured()) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare ProfessionalWeb is not configured (integration.lifecare-professionalweb.url is unset)");
		}
		seedConfiguration();
		authenticate();

		final var headers = new LinkedHashMap<String, String>();
		headers.put("Cookie", cookies.toHeader());
		cookies.get(SIGNED_IN_COOKIE).ifPresent(token -> headers.put("X-LEGACY-TOKEN", token));
		return headers;
	}

	/**
	 * Asks the identity portal to hand {@code module} an artifact of its own. Called when a module has said it wants
	 * one, never speculatively.
	 *
	 * @param module the Lifecare module
	 */
	public void bootstrapModule(final String module) {
		synchronized (signInLock) {
			if (bootstrappedModules.contains(module)) {
				return;
			}
			final var artifact = http.followRedirects(properties.baseUrl() + "/" + module + "/Heartbeat", cookies, null);
			LOG.info("Lifecare handed {} its artifact ({})", module, artifact.describe());
			if (artifact.status() != 200) {
				throw Problem.valueOf(BAD_GATEWAY, "Lifecare would not start a session for " + module + " (" + artifact.describe() + ")");
			}
			bootstrappedModules.add(module);
		}
	}

	/**
	 * Takes in cookies Lifecare rotated on an ordinary call, so the session stays current.
	 *
	 * @param response the response
	 */
	public void absorb(final ProfessionalWebResponse response) {
		cookies.absorbSetCookie(response.setCookies());
	}

	/**
	 * Throws the session away so the next call builds a new one. The module bootstraps go with it: their artifacts were
	 * issued against the session that died.
	 */
	public void reset() {
		synchronized (signInLock) {
			cookies.clear();
			bootstrappedModules.clear();
			establishedAt = null;
		}
	}

	/**
	 * Throws the session away, but only if it is still the one a failing request used. Several requests that found the
	 * same session dead then renew it once: the first resets and signs in, and the rest find a newer session and just
	 * resend on it, instead of each throwing away the one the previous request had just established.
	 *
	 * @param seen the session the failing request used, as {@link #established()} returned it
	 */
	public void reset(final Instant seen) {
		synchronized (signInLock) {
			if (Objects.equals(establishedAt, seen)) {
				reset();
			}
		}
	}

	/**
	 * When the current session was established — also what tells one session from the next.
	 *
	 * @return the time, or null when no session is held
	 */
	public Instant established() {
		return establishedAt;
	}

	/**
	 * Whether a signed-in session is currently held. For health reporting; says nothing about whether Lifecare still
	 * accepts it.
	 *
	 * @return true when a session is held
	 */
	public boolean isEstablished() {
		return establishedAt != null && cookies.has(SIGNED_IN_COOKIE);
	}

	/**
	 * The configuration cookies the identity portal normally picks up off the query string on a first visit. Nothing
	 * issues them to a server-side caller, so they are set directly.
	 */
	private void seedConfiguration() {
		setIfPresent("metadomain", properties.domain());
		setIfPresent("actor", properties.actor());
		setIfPresent("idpmethod", properties.idpMethod());
		setIfPresent("federationprofile", properties.federationProfile());
	}

	private void setIfPresent(final String name, final String value) {
		if (value != null && !value.isBlank()) {
			cookies.set(name, value);
		}
	}

	private boolean isExpired() {
		final var established = establishedAt;
		return established != null && !clock.instant().isBefore(established.plus(properties.sessionTtl()));
	}

	private void authenticate() {
		if (isEstablished() && !isExpired()) {
			return;
		}
		synchronized (signInLock) {
			// Whoever held the lock before us may already have signed in (or renewed a session someone else found expired).
			if (isEstablished() && !isExpired()) {
				return;
			}
			if (isExpired()) {
				LOG.info("Lifecare session has passed its TTL - signing in again");
			}
			LOG.info("Establishing a Lifecare session as the integration account");
			// Start from nothing: a failed attempt (or an expired session) may have left half a flow's cookies behind.
			cookies.clear();
			bootstrappedModules.clear();
			establishedAt = null;
			seedConfiguration();
			signIn.signIn(cookies);
			establishedAt = clock.instant();
			// Names only, never values.
			LOG.info("Lifecare session holds: {}", String.join(", ", cookies.names()));
		}
	}
}
