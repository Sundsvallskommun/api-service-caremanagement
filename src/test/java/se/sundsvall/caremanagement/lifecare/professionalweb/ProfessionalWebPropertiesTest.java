package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalWebPropertiesTest {

	@Test
	void toStringRedactsUsernameAndPassword() {
		final var properties = new ProfessionalWebProperties("https://lifecare.example.se", "SundsvallVoO_PLUS", "Actor_Professional", "saml",
			"Sundsvall_Intra", "joe01doe", "s3cret", Duration.ofMinutes(20), 5, 30, "template-1", "template-2", null);

		final var text = properties.toString();

		assertThat(text)
			.doesNotContain("joe01doe", "s3cret")
			.contains("username=***", "password=***", "url=https://lifecare.example.se");
	}

	@Test
	void toStringHandlesUnsetCredentials() {
		final var properties = new ProfessionalWebProperties(null, null, "Actor_Professional", "saml", null, null, null, Duration.ofMinutes(20), 5, 30, "template-1", "template-2", null);

		assertThat(properties.toString()).contains("username=null", "password=null");
	}

	@Test
	void baseUrlDropsEveryTrailingSlash() {
		assertThat(withUrl("https://lifecare.example.se").baseUrl()).isEqualTo("https://lifecare.example.se");
		assertThat(withUrl("https://lifecare.example.se/").baseUrl()).isEqualTo("https://lifecare.example.se");
		assertThat(withUrl("https://lifecare.example.se/fc///").baseUrl()).isEqualTo("https://lifecare.example.se/fc");
	}

	@Test
	void baseUrlIsEmptyWhenUnconfigured() {
		assertThat(withUrl(null).baseUrl()).isEmpty();
	}

	private static ProfessionalWebProperties withUrl(final String url) {
		return new ProfessionalWebProperties(url, null, "Actor_Professional", "saml", null, null, null, Duration.ofMinutes(20), 5, 30, "template-1", "template-2", null);
	}
}
