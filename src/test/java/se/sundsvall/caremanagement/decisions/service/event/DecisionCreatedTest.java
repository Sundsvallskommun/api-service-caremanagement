package se.sundsvall.caremanagement.decisions.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionCreatedTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void accessors() {
		final var timestamp = FIXED_TIMESTAMP;
		final var event = new DecisionCreated("decision-1", "errand-1", "2281", "my-namespace", "PAYMENT",
			"APPROVED", "decider", timestamp);

		assertThat(event.decisionId()).isEqualTo("decision-1");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("my-namespace");
		assertThat(event.decisionType()).isEqualTo("PAYMENT");
		assertThat(event.outcome()).isEqualTo("APPROVED");
		assertThat(event.decidedBy()).isEqualTo("decider");
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}
}
