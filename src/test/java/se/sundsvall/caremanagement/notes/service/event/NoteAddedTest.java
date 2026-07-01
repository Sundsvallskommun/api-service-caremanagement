package se.sundsvall.caremanagement.notes.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteAddedTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void accessors() {
		final var timestamp = FIXED_TIMESTAMP;
		final var event = new NoteAdded("note-1", "errand-1", "2281", "my-namespace", "author-1", timestamp);

		assertThat(event.noteId()).isEqualTo("note-1");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("my-namespace");
		assertThat(event.author()).isEqualTo("author-1");
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}
}
