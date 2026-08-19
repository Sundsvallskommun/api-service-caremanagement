package se.sundsvall.caremanagement.journal.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateJournalEntryTest {
	private static final OffsetDateTime ENTRY_DATE_TIME = OffsetDateTime.parse("2025-05-30T14:30:00+02:00");

	@Test
	void testAccessors() {
		final var request = new UpdateJournalEntry("Journalfört meddelande", "Rubrik", "body", ENTRY_DATE_TIME, "editor");

		assertThat(request.type()).isEqualTo("Journalfört meddelande");
		assertThat(request.heading()).isEqualTo("Rubrik");
		assertThat(request.text()).isEqualTo("body");
		assertThat(request.entryDateTime()).isEqualTo(ENTRY_DATE_TIME);
		assertThat(request.modifiedBy()).isEqualTo("editor");
	}

	@Test
	void testOptionalFieldsMayBeNull() {
		final var request = new UpdateJournalEntry("T", "H", null, ENTRY_DATE_TIME, null);

		assertThat(request.text()).isNull();
		assertThat(request.modifiedBy()).isNull();
	}
}
