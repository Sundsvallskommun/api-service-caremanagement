package se.sundsvall.caremanagement.referral.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralResponseRequestTest {

	@Test
	void accessor() {
		final var request = new ReferralResponseRequest("The authority has no objection.");

		assertThat(request.responseText()).isEqualTo("The authority has no objection.");
	}
}
