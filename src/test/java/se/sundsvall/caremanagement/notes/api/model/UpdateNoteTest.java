package se.sundsvall.caremanagement.notes.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateNoteTest {

	@Test
	void accessors() {
		final var note = new UpdateNote("updated content", "editor-1");

		assertThat(note.body()).isEqualTo("updated content");
		assertThat(note.modifiedBy()).isEqualTo("editor-1");
	}

	@Test
	void modifiedByIsOptional() {
		final var note = new UpdateNote("updated content", null);
		assertThat(note.body()).isEqualTo("updated content");
		assertThat(note.modifiedBy()).isNull();
	}
}
