package se.sundsvall.caremanagement.statushistory.integration.db.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusHistoryEntityTest {

	@Test
	void builderMethods() {
		final var changedAt = OffsetDateTime.now();
		final var entity = StatusHistoryEntity.create()
			.withId("id")
			.withErrandId("errand-1")
			.withFromStatus("OPEN")
			.withToStatus("CLOSED")
			.withChangedBy("user")
			.withChangedAt(changedAt);

		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getFromStatus()).isEqualTo("OPEN");
		assertThat(entity.getToStatus()).isEqualTo("CLOSED");
		assertThat(entity.getChangedBy()).isEqualTo("user");
		assertThat(entity.getChangedAt()).isEqualTo(changedAt);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(StatusHistoryEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatusHistoryEntity()).hasAllNullFieldsOrProperties();
	}
}
