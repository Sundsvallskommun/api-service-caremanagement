package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class FinancialAssistanceProcessStartRetryPropertiesTest {

	@Autowired
	private FinancialAssistanceProcessStartRetryProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.enabled()).isTrue();
		assertThat(properties.municipalityId()).isEqualTo("2281");
		assertThat(properties.namespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(properties.minAge()).isEqualTo(Duration.ofMinutes(10));
		assertThat(properties.maxAge()).isEqualTo(Duration.ofDays(7));
	}
}
