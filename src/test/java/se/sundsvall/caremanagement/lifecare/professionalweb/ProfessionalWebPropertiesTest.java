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

		assertThat(text).doesNotContain("joe01doe", "s3cret");
		assertThat(text).contains("username=***", "password=***", "url=https://lifecare.example.se");
	}

	@Test
	void toStringHandlesUnsetCredentials() {
		final var properties = new ProfessionalWebProperties(null, null, "Actor_Professional", "saml", null, null, null, Duration.ofMinutes(20), 5, 30, "template-1", "template-2", null);

		assertThat(properties.toString()).contains("username=null", "password=null");
	}
}
