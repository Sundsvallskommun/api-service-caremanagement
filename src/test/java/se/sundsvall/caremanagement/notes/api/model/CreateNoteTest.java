package se.sundsvall.caremanagement.notes.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateNoteTest {

	@Test
	void accessors() {
		final var note = new CreateNote("body content", "author-1");

		assertThat(note.body()).isEqualTo("body content");
		assertThat(note.author()).isEqualTo("author-1");
	}

	@Test
	void authorIsOptional() {
		final var note = new CreateNote("body content", null);
		assertThat(note.body()).isEqualTo("body content");
		assertThat(note.author()).isNull();
	}
}
