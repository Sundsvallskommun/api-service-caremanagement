package se.sundsvall.caremanagement.decisions.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionRecordedTest {

	@Test
	void accessors() {
		final var timestamp = OffsetDateTime.now();
		final var event = new DecisionRecorded("decision-1", "errand-1", "type-slug",
			"APPROVED", "decider", timestamp);

		assertThat(event.decisionId()).isEqualTo("decision-1");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.typeSlug()).isEqualTo("type-slug");
		assertThat(event.outcome()).isEqualTo("APPROVED");
		assertThat(event.decidedBy()).isEqualTo("decider");
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}
}
