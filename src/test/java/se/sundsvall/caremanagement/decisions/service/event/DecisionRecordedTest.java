package se.sundsvall.caremanagement.decisions.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionRecordedTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void accessors() {
		final var timestamp = FIXED_TIMESTAMP;
		final var event = new DecisionRecorded("decision-1", "errand-1", "2281", "my-namespace", "type-slug",
			"APPROVED", "decider", timestamp);

		assertThat(event.decisionId()).isEqualTo("decision-1");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("my-namespace");
		assertThat(event.typeSlug()).isEqualTo("type-slug");
		assertThat(event.outcome()).isEqualTo("APPROVED");
		assertThat(event.decidedBy()).isEqualTo("decider");
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}
}
