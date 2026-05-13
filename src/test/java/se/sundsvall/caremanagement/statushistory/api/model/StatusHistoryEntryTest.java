package se.sundsvall.caremanagement.statushistory.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusHistoryEntryTest {

	@Test
	void accessors() {
		final var changedAt = OffsetDateTime.now();
		final var entry = new StatusHistoryEntry("id-1", "errand-1", "OPEN", "CLOSED", "user-1", changedAt);

		assertThat(entry.id()).isEqualTo("id-1");
		assertThat(entry.errandId()).isEqualTo("errand-1");
		assertThat(entry.fromStatus()).isEqualTo("OPEN");
		assertThat(entry.toStatus()).isEqualTo("CLOSED");
		assertThat(entry.changedBy()).isEqualTo("user-1");
		assertThat(entry.changedAt()).isEqualTo(changedAt);
	}

	@Test
	void initialTransitionHasNullFromStatus() {
		final var entry = new StatusHistoryEntry("id", "e", null, "OPEN", null, OffsetDateTime.now());
		assertThat(entry.fromStatus()).isNull();
		assertThat(entry.changedBy()).isNull();
	}
}
