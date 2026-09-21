package se.sundsvall.caremanagement.lifecare.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Pins the catalogue names verksamheten gave for an EB återansökan (Regelverk Drakel, 2026-09-21). They are what the
 * assembler matches Lifecare's catalogue on, so a typo here is an actualisation created with the wrong type.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class ActualisationPropertiesTest {

	@Autowired
	private ActualisationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.type()).isEqualTo("Ek Återansökan Digital Ekonomiskt bistånd");
		assertThat(properties.fromWho()).isEqualTo("Den enskilde");
		assertThat(properties.reason()).isEqualTo("Ekonomiskt bistånd");
		assertThat(properties.organisation()).isEqualTo("Ekonomiskt bistånd");
	}
}
