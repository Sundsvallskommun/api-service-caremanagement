package se.sundsvall.caremanagement.notes.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteAddedTest {

	@Test
	void accessors() {
		final var timestamp = OffsetDateTime.now();
		final var event = new NoteAdded("note-1", "errand-1", "author-1", timestamp);

		assertThat(event.noteId()).isEqualTo("note-1");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.author()).isEqualTo("author-1");
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}
}
