package se.sundsvall.caremanagement.lifecare.integration.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class LifecareFamilyCarePropertiesTest {

	@Autowired
	private LifecareFamilyCareProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.url()).isEqualTo("http://lifecarefamilycare.url");
		assertThat(properties.domain()).isEqualTo("junit-domain");
		assertThat(properties.key()).isEqualTo("junit-key");
		assertThat(properties.userKey()).isEqualTo("junit-user-key");
		assertThat(properties.userKeyOrDefault()).isEqualTo("junit-user-key");
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}

	/** The separate user-directory licence key is optional — unset or blank falls back to the main key. */
	@Test
	void testUserKeyFallsBackToKeyWhenNotConfigured() {
		assertThat(new LifecareFamilyCareProperties("url", "domain", "the-key", null, 5, 30).userKeyOrDefault()).isEqualTo("the-key");
		assertThat(new LifecareFamilyCareProperties("url", "domain", "the-key", " ", 5, 30).userKeyOrDefault()).isEqualTo("the-key");
	}
}
