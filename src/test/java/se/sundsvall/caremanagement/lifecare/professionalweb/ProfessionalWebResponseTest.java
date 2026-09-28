package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalWebResponseTest {

	private static HttpHeaders headers(final Map<String, List<String>> values) {
		return HttpHeaders.of(values, (name, value) -> true);
	}

	@Test
	void nullBodyBecomesAnEmptyArray() {
		final var response = new ProfessionalWebResponse(200, headers(Map.of()), null, URI.create("https://example.com"));

		assertThat(response.body()).isEmpty();
	}

	@Test
	void isSuccessIsTrueForA2xxStatus() {
		assertThat(new ProfessionalWebResponse(200, headers(Map.of()), null, URI.create("https://example.com")).isSuccess()).isTrue();
		assertThat(new ProfessionalWebResponse(404, headers(Map.of()), null, URI.create("https://example.com")).isSuccess()).isFalse();
	}

	@Test
	void locationAndContentTypeReadTheirHeaders() {
		final var response = new ProfessionalWebResponse(302, headers(Map.of(
			"Location", List.of("/next"),
			"Content-Type", List.of("text/html; charset=utf-8"))), null, URI.create("https://example.com"));

		assertThat(response.location()).contains("/next");
		assertThat(response.contentType()).isEqualTo("text/html; charset=utf-8");
	}

	@Test
	void contentTypeIsEmptyWhenMissing() {
		final var response = new ProfessionalWebResponse(200, headers(Map.of()), null, URI.create("https://example.com"));

		assertThat(response.contentType()).isEmpty();
		assertThat(response.location()).isEmpty();
	}

	@Test
	void setCookiesReadsEveryValue() {
		final var response = new ProfessionalWebResponse(200, headers(Map.of("Set-Cookie", List.of("a=1", "b=2"))), null, URI.create("https://example.com"));

		assertThat(response.setCookies()).containsExactly("a=1", "b=2");
	}

	@Test
	void bodyAsStringDecodesUtf8() {
		final var response = new ProfessionalWebResponse(200, headers(Map.of()), "hej".getBytes(StandardCharsets.UTF_8), URI.create("https://example.com"));

		assertThat(response.bodyAsString()).isEqualTo("hej");
	}

	@Test
	void describeIncludesStatusTypeAndRedirect() {
		final var response = new ProfessionalWebResponse(302, headers(Map.of(
			"Content-Type", List.of("text/html; charset=utf-8"),
			"Location", List.of("/next"))), null, URI.create("https://example.com"));

		assertThat(response.describe()).isEqualTo("status 302, type text/html, redirects to /next");
	}

	@Test
	void describeOmitsTypeAndRedirectWhenAbsent() {
		final var response = new ProfessionalWebResponse(200, headers(Map.of()), null, URI.create("https://example.com"));

		assertThat(response.describe()).isEqualTo("status 200");
	}

	@Test
	void equalsAndHashCodeCompareBodyByContentNotIdentity() {
		final var uri = URI.create("https://example.com");
		final var first = new ProfessionalWebResponse(200, headers(Map.of()), new byte[] {
			1, 2, 3
		}, uri);
		final var second = new ProfessionalWebResponse(200, headers(Map.of()), new byte[] {
			1, 2, 3
		}, uri);
		final var different = new ProfessionalWebResponse(200, headers(Map.of()), new byte[] {
			9
		}, uri);

		assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(different);
		assertThat(first).isEqualTo(first);
		assertThat(first).isNotEqualTo("not a response");
		assertThat(first).isNotEqualTo(null);
	}

	@Test
	void toStringSummarizesBodyLengthRatherThanContent() {
		final var response = new ProfessionalWebResponse(200, headers(Map.of()), new byte[] {
			1, 2, 3, 4
		}, URI.create("https://example.com"));

		assertThat(response.toString()).contains("body=4 bytes").doesNotContain("1, 2, 3, 4");
	}
}
