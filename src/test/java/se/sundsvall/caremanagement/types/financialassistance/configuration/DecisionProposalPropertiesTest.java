package se.sundsvall.caremanagement.types.financialassistance.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class DecisionProposalPropertiesTest {

	@Autowired
	private DecisionProposalProperties properties;

	@Test
	void testProperties() {
		// application-junit.yml overrides the default of 36, so a binding that silently fell back would show here.
		assertThat(properties.recoveryClaimLookbackMonths()).isEqualTo(24);
	}
}
