package se.sundsvall.caremanagement.lifecare.integration.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class LifecareFcPropertiesTest {

	@Autowired
	private LifecareFcProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.url()).isEqualTo("http://lifecarefc.url");
		assertThat(properties.domain()).isEqualTo("junit-domain");
		assertThat(properties.key()).isEqualTo("junit-key");
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
