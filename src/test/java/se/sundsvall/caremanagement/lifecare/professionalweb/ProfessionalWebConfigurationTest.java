package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.security.Truststore;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalWebConfigurationTest {

	@Test
	void professionalWebHttpClientNeverFollowsRedirectsAndPinsHttp11() {
		final var properties = new ProfessionalWebProperties("https://lifecare.example.se", "SundsvallVoO_PLUS", "Actor_Professional", "saml",
			"Sundsvall_Intra", "joe01doe", "s3cret", Duration.ofMinutes(20), 5, 30, "template-1", "template-2", null);

		final var client = new ProfessionalWebConfiguration().professionalWebHttpClient(properties, new Truststore(null));

		assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
		assertThat(client.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
		assertThat(client.connectTimeout()).contains(Duration.ofSeconds(properties.connectTimeout()));
		assertThat(client.sslContext()).isNotNull();
	}
}
