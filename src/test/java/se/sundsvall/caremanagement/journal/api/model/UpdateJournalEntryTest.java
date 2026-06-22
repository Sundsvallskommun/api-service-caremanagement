package se.sundsvall.caremanagement.journal.api.model;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateJournalEntryTest {
	private static final LocalDate ENTRY_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime ENTRY_TIME = LocalTime.of(14, 30);

	@Test
	void accessors() {
		final var request = new UpdateJournalEntry("Journalfört meddelande", "Rubrik", "body", ENTRY_DATE, ENTRY_TIME, "editor");

		assertThat(request.type()).isEqualTo("Journalfört meddelande");
		assertThat(request.heading()).isEqualTo("Rubrik");
		assertThat(request.text()).isEqualTo("body");
		assertThat(request.entryDate()).isEqualTo(ENTRY_DATE);
		assertThat(request.entryTime()).isEqualTo(ENTRY_TIME);
		assertThat(request.modifiedBy()).isEqualTo("editor");
	}

	@Test
	void optionalFieldsMayBeNull() {
		final var request = new UpdateJournalEntry("T", "H", null, ENTRY_DATE, null, null);

		assertThat(request.text()).isNull();
		assertThat(request.entryTime()).isNull();
		assertThat(request.modifiedBy()).isNull();
	}
}
