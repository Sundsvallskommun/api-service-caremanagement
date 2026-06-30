package se.sundsvall.caremanagement.eventlog.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandEventCountTest {

	@Test
	void accessor() {
		assertThat(new ErrandEventCount(12).count()).isEqualTo(12);
	}
}
