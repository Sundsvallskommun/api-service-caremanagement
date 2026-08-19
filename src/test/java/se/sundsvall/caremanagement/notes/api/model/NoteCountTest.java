package se.sundsvall.caremanagement.notes.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteCountTest {

	@Test
	void testAccessor() {
		assertThat(new NoteCount(4).count()).isEqualTo(4);
	}
}
