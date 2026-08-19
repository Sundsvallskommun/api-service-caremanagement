package se.sundsvall.caremanagement.statistics.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusCountTest {

	@Test
	void testAccessors() {
		final var statusCount = new StatusCount("DECIDED", 12L);

		assertThat(statusCount.status()).isEqualTo("DECIDED");
		assertThat(statusCount.count()).isEqualTo(12L);
	}
}
