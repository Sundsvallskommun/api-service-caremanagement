package se.sundsvall.caremanagement.financialaid.integration.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAidPropertiesTest {

	@Test
	void accessors() {
		final var properties = new FinancialAidProperties(5, 30);

		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
	}
}
