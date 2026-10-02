package se.sundsvall.caremanagement.cocaseworkers.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddCoCaseworkerTest {

	@Test
	void testAccessors() {
		final var request = new AddCoCaseworker("jane01doe");

		assertThat(request.userId()).isEqualTo("jane01doe");
	}
}
