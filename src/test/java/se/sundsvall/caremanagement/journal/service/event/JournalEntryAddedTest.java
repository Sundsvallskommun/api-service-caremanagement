package se.sundsvall.caremanagement.journal.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryAddedTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void accessors() {
		final var event = new JournalEntryAdded("je-1", "errand-1", "2281", "my-namespace", "Journalfört meddelande", "carola", FIXED_TIMESTAMP);

		assertThat(event.journalEntryId()).isEqualTo("je-1");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("my-namespace");
		assertThat(event.type()).isEqualTo("Journalfört meddelande");
		assertThat(event.createdBy()).isEqualTo("carola");
		assertThat(event.timestamp()).isEqualTo(FIXED_TIMESTAMP);
	}
}
