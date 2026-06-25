package se.sundsvall.caremanagement.stakeholders.service.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StakeholderMutatedTest {

	@Test
	void accessors() {
		final var event = new StakeholderMutated("2281", "MY_NAMESPACE", "errand-1");

		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("MY_NAMESPACE");
		assertThat(event.errandId()).isEqualTo("errand-1");
	}
}
