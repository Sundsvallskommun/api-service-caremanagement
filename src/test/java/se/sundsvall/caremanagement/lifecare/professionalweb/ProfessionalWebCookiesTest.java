package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalWebCookiesTest {

	@Test
	void absorbSetsAndOverwrites() {
		final var cookies = new ProfessionalWebCookies();

		cookies.absorbSetCookie(List.of("ASP.NET_SessionId=abc; path=/; HttpOnly", "LEGACY-TOKEN=a=b=c; secure"));
		cookies.absorbSetCookie(List.of("ASP.NET_SessionId=def; path=/"));

		assertThat(cookies.get("ASP.NET_SessionId")).contains("def");
		assertThat(cookies.get("LEGACY-TOKEN")).contains("a=b=c");
		assertThat(cookies.toHeader()).isEqualTo("ASP.NET_SessionId=def; LEGACY-TOKEN=a=b=c");
		assertThat(cookies.names()).containsExactly("ASP.NET_SessionId", "LEGACY-TOKEN");
	}

	@Test
	void absorbHonoursDeletions() {
		final var cookies = new ProfessionalWebCookies();
		cookies.set("one", "1");
		cookies.set("two", "2");
		cookies.set("three", "3");

		cookies.absorbSetCookie(List.of(
			"one=; Max-Age=0",
			"two=; expires=Thu, 01-Jan-1970 00:00:00 GMT; path=/",
			"three=kept; Max-Age=3600"));

		assertThat(cookies.has("one")).isFalse();
		assertThat(cookies.has("two")).isFalse();
		assertThat(cookies.get("three")).contains("kept");
	}

	@Test
	void absorbIgnoresMalformedHeadersAndAttributes() {
		final var cookies = new ProfessionalWebCookies();

		cookies.absorbSetCookie(List.of("no-separator", "=novalue", "name=value; Max-Age=soon; expires=not a date; flag"));

		assertThat(cookies.names()).containsExactly("name");
	}

	@Test
	void clear() {
		final var cookies = new ProfessionalWebCookies();
		cookies.set("name", "value");

		cookies.clear();

		assertThat(cookies.toHeader()).isEmpty();
		assertThat(cookies.get("name")).isEmpty();
	}
}
