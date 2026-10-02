package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignInFormTest {

	private static final String PAGE = "https://idp.example/samlv2/idp/login";

	@Test
	void mobilityGuardLoginUsesTheHiddenCredentialFields() {
		final var html = """
			<form action="/samlv2/idp/login/post" method="post">
			  <input type="hidden" name="uid" value="">
			  <input type="hidden" name="otp" value="">
			  <input type="hidden" name="state" value="s1">
			  <input type="submit" name="go" value="Logga in">
			</form>
			<input type="text" name="visible-user">
			""";

		final var form = SignInForm.extract(html, PAGE).orElseThrow();

		assertThat(form.isSignIn()).isTrue();
		assertThat(form.action()).isEqualTo("https://idp.example/samlv2/idp/login/post");
		assertThat(form.usernameField()).isEqualTo("uid");
		assertThat(form.passwordField()).isEqualTo("otp");
		assertThat(form.fields()).containsOnlyKeys("uid", "otp", "state");
	}

	@Test
	void ordinaryLoginPrefersTheFormWithAPasswordAndANamedUsernameField() {
		final var html = """
			<form action="/search"><input type="text" name="q"></form>
			<form action=https://idp.example/login?x=1>
			  <input type="text" name="language">
			  <input type="text" name="j_username">
			  <input type="password" name="j_password">
			</form>
			""";

		final var form = SignInForm.extract(html, PAGE).orElseThrow();

		assertThat(form.action()).isEqualTo("https://idp.example/login?x=1");
		assertThat(form.usernameField()).isEqualTo("j_username");
		assertThat(form.passwordField()).isEqualTo("j_password");
	}

	@Test
	void loginWithoutAHintedNameFallsBackToTheFirstTextField() {
		final var html = """
			<form action=""><input type="email" name="e"><input type="password" name="p"></form>
			""";

		final var form = SignInForm.extract(html, PAGE).orElseThrow();

		assertThat(form.action()).isEqualTo(PAGE);
		assertThat(form.usernameField()).isEqualTo("e");
	}

	@Test
	void samlPostBackCarriesTheTextarea() {
		final var html = """
			<form method="post" action="https://lifecare.example/IdentityPortalWeb/acs">
			  <textarea name="SAMLResponse">
			    PHNhbWw+
			  </textarea>
			  <input type="hidden" name="RelayState" value="r">
			</form>
			""";

		final var form = SignInForm.extract(html, PAGE).orElseThrow();

		assertThat(form.isSignIn()).isFalse();
		assertThat(form.usernameField()).isNull();
		assertThat(form.fields()).containsEntry("SAMLResponse", "PHNhbWw+").containsEntry("RelayState", "r");
	}

	@Test
	void pageWithoutAForm() {
		assertThat(SignInForm.extract("<html><body>Hej</body></html>", PAGE)).isEmpty();
	}
}
