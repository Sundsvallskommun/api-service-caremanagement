package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

/**
 * One HTML form of the Lifecare sign-in flow.
 *
 * <p>
 * Two hops of the flow are forms rather than redirects: the identity provider's sign-in page, and the page that posts
 * the SAML response back to Lifecare (a browser submits that one with script the moment it loads). Both are read off
 * the page rather than hardcoded, because their field names belong to the identity provider and change without asking
 * us.
 * </p>
 *
 * @param action        absolute URL the form posts to
 * @param fields        every field the form carries, hidden state included, ready to be posted back
 * @param passwordField the name of the password field, when this form is a sign-in form
 * @param usernameField the name of the field the username belongs in, when this form is a sign-in form
 */
public record SignInForm(String action, Map<String, String> fields, String passwordField, String usernameField) {

	/**
	 * Where MobilityGuard's web login actually posts the credentials. Its visible boxes sit outside the form next to a
	 * scrambled keypad; on submit the page script copies both values verbatim into these two hidden fields, so filling
	 * them is exactly what the page itself sends.
	 */
	static final String MOBILITYGUARD_USERNAME = "uid";
	static final String MOBILITYGUARD_PASSWORD = "otp";

	private static final Pattern USERNAME_HINT = Pattern.compile("user|login|uid|account", Pattern.CASE_INSENSITIVE);
	private static final List<String> IGNORED_TYPES = List.of("submit", "button", "reset", "image");

	public boolean isSignIn() {
		return passwordField != null;
	}

	/**
	 * Reads the form a page carries the flow forward with.
	 *
	 * <p>
	 * A sign-in page is rarely one form, so the form holding a password field wins when there is one; the first form is
	 * the fallback for the pages that merely post a SAML assertion onwards.
	 * </p>
	 *
	 * @param  html    the page as delivered
	 * @param  pageUrl the URL it was fetched from, so a relative or empty action resolves correctly
	 * @return         the form, or empty when the page has none
	 */
	public static Optional<SignInForm> extract(final String html, final String pageUrl) {
		final var forms = Jsoup.parse(html, pageUrl).select("form").stream()
			.map(form -> read(form, pageUrl))
			.toList();

		return forms.stream()
			.filter(SignInForm::isSignIn)
			.findFirst()
			.or(() -> forms.stream().findFirst());
	}

	private static SignInForm read(final Element form, final String pageUrl) {
		final var fields = new LinkedHashMap<String, String>();
		String passwordField = null;
		String firstTextField = null;
		String hintedUsernameField = null;

		for (final var input : form.select("input[name]")) {
			final var name = input.attr("name");
			final var type = input.attr("type").toLowerCase(Locale.ROOT);
			if (!StringUtils.hasText(name) || IGNORED_TYPES.contains(type)) {
				continue;
			}
			fields.put(name, input.attr("value"));

			if ("password".equals(type)) {
				passwordField = Optional.ofNullable(passwordField).orElse(name);
			} else if (!"hidden".equals(type)) {
				hintedUsernameField = firstHint(hintedUsernameField, name);
				firstTextField = firstText(firstTextField, name, type);
			}
		}
		// The identity provider hands the SAML response back in textareas, not inputs.
		for (final var textarea : form.select("textarea[name]")) {
			fields.put(textarea.attr("name"), textarea.wholeText().trim());
		}

		final var action = resolve(form.attr("action"), pageUrl);

		if (fields.containsKey(MOBILITYGUARD_USERNAME) && fields.containsKey(MOBILITYGUARD_PASSWORD)) {
			return new SignInForm(action, fields, MOBILITYGUARD_PASSWORD, MOBILITYGUARD_USERNAME);
		}
		if (passwordField == null) {
			return new SignInForm(action, fields, null, null);
		}
		return new SignInForm(action, fields, passwordField, Optional.ofNullable(hintedUsernameField).orElse(firstTextField));
	}

	private static String firstHint(final String current, final String name) {
		if (current == null && USERNAME_HINT.matcher(name).find()) {
			return name;
		}
		return current;
	}

	private static String firstText(final String current, final String name, final String type) {
		if (current == null && List.of("text", "email", "").contains(type)) {
			return name;
		}
		return current;
	}

	/** An empty action means the form posts back to the page it came from. */
	private static String resolve(final String action, final String pageUrl) {
		if (!StringUtils.hasText(action)) {
			return pageUrl;
		}
		return URI.create(pageUrl).resolve(action.trim()).toString();
	}
}
