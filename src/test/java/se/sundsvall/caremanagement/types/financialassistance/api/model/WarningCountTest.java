package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarningCountTest {

	@Test
	void accessor() {
		assertThat(new WarningCount(3).count()).isEqualTo(3);
	}
}
