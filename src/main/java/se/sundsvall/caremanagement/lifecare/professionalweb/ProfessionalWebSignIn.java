package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebCookies.SIGNED_IN_COOKIE;

/**
 * Signs careM in to Lifecare as its integration account.
 *
 * <p>
 * Walks exactly the flow a caseworker's browser walks, only without a browser: open Lifecare's entry URL, follow the
 * redirects into the identity provider (MobilityGuard), submit the sign-in form, and post the resulting SAML assertion
 * back to Lifecare. Each hop's cookies are carried into the next, and what falls out at the end is a session Lifecare
 * recognises, proven by the LEGACY-TOKEN cookie.
 * </p>
 *
 * <p>
 * What it cannot do is answer a second factor. If the account is enrolled for a one-time code, or the identity
 * provider waits for something outside the page, the flow stops on a page it cannot satisfy and this throws.
 * </p>
 */
@Component
public class ProfessionalWebSignIn {

	private static final Logger LOG = LoggerFactory.getLogger(ProfessionalWebSignIn.class);

	/** Post the SAML request onwards, sign in, post the assertion back: three hops, plus slack for one more. */
	private static final int MAX_FORM_HOPS = 6;

	private final ProfessionalWebProperties properties;
	private final ProfessionalWebHttp http;

	public ProfessionalWebSignIn(final ProfessionalWebProperties properties, final ProfessionalWebHttp http) {
		this.properties = properties;
		this.http = http;
	}

	/**
	 * Puts a signed-in session into {@code cookies}, or throws.
	 *
	 * @param cookies the cookie store to fill
	 */
	public void signIn(final ProfessionalWebCookies cookies) {
		if (!properties.hasAccount()) {
			throw Problem.valueOf(BAD_GATEWAY, "No Lifecare account configured (integration.lifecare-professionalweb.username/password)");
		}

		var response = http.followRedirects(entryUrl(), cookies, null);
		var credentialsSent = false;
		String lastAction = null;

		for (var hop = 0; hop < MAX_FORM_HOPS && !cookies.has(SIGNED_IN_COOKIE); hop++) {
			final var form = SignInForm.extract(response.bodyAsString(), response.uri().toString());
			if (form.isEmpty()) {
				break;
			}
			final var action = form.get().action();
			if (action.equals(lastAction)) {
				// A page that posts itself back to the same place is a waiting room, not a step: the identity provider is
				// polling while something outside the page authenticates. Posting again only hammers it.
				throw Problem.valueOf(BAD_GATEWAY, "The identity provider is waiting at " + URI.create(action).getPath()
					+ " rather than asking for a password - the account is not being offered password sign-in");
			}
			lastAction = action;

			final var fields = new LinkedHashMap<>(form.get().fields());
			if (form.get().isSignIn()) {
				credentialsSent = fillCredentials(form.get(), fields, credentialsSent);
			}

			// Names the step without naming what is in it, so a flow that stalls says where it stalled.
			LOG.info("Lifecare sign-in: posting {} to {}{}", describeStep(form.get()), URI.create(action).getHost(), URI.create(action).getPath());
			response = http.followRedirects(action, cookies, fields);
		}

		if (!cookies.has(SIGNED_IN_COOKIE)) {
			if (credentialsSent) {
				throw Problem.valueOf(BAD_GATEWAY, "Lifecare never issued a session after the integration account signed in");
			}
			throw Problem.valueOf(BAD_GATEWAY, "The Lifecare sign-in page had no password field - the flow may be asking for a one-time code");
		}
		LOG.info("Lifecare accepted the integration account");
	}

	private boolean fillCredentials(final SignInForm form, final LinkedHashMap<String, String> fields, final boolean credentialsSent) {
		if (credentialsSent) {
			// Asked a second time: either the credentials are wrong, or the account is asked for something a server
			// cannot give.
			throw Problem.valueOf(BAD_GATEWAY,
				"Lifecare asked the integration account to sign in a second time - check the credentials, or whether the account is enrolled for a one-time code");
		}
		if (form.usernameField() == null) {
			throw Problem.valueOf(BAD_GATEWAY, "Could not tell which field the Lifecare username belongs in on the sign-in page");
		}
		fields.put(form.usernameField(), properties.username());
		fields.put(form.passwordField(), properties.password());
		return true;
	}

	private static String describeStep(final SignInForm form) {
		if (form.isSignIn()) {
			return "credentials";
		}
		return "the flow onwards";
	}

	/** Lifecare's entry point for a caseworker, the URL a browser is pointed at to start the flow. */
	String entryUrl() {
		return properties.baseUrl() + "/WE.Flow.Html"
			+ "?domain=" + encode(properties.domain())
			+ "&Actor=" + encode(properties.actor())
			+ "&IDPMethod=" + encode(properties.idpMethod());
	}

	private static String encode(final String value) {
		if (value == null) {
			return "";
		}
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
