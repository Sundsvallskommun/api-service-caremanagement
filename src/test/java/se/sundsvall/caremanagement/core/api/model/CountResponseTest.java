package se.sundsvall.caremanagement.core.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CountResponseTest {

	@Test
	void testAccessors() {
		final var countResponse = new CountResponse(42L);

		assertThat(countResponse.count()).isEqualTo(42L);
	}
}
