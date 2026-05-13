package se.sundsvall.caremanagement.notes.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteTest {

	@Test
	void builderMethods() {
		final var id = "n1";
		final var errandId = "e1";
		final var body = "body";
		final var author = "author";
		final var created = OffsetDateTime.now();

		final var note = Note.create()
			.withId(id)
			.withErrandId(errandId)
			.withBody(body)
			.withAuthor(author)
			.withCreated(created);

		assertThat(note.getId()).isEqualTo(id);
		assertThat(note.getErrandId()).isEqualTo(errandId);
		assertThat(note.getBody()).isEqualTo(body);
		assertThat(note.getAuthor()).isEqualTo(author);
		assertThat(note.getCreated()).isEqualTo(created);
	}

	@Test
	void setters() {
		final var note = Note.create();
		note.setId("id");
		note.setErrandId("eid");
		note.setBody("b");
		note.setAuthor("a");
		final var ts = OffsetDateTime.now();
		note.setCreated(ts);

		assertThat(note.getId()).isEqualTo("id");
		assertThat(note.getErrandId()).isEqualTo("eid");
		assertThat(note.getBody()).isEqualTo("b");
		assertThat(note.getAuthor()).isEqualTo("a");
		assertThat(note.getCreated()).isEqualTo(ts);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(Note.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var ts = OffsetDateTime.now();
		final var a = Note.create().withId("1").withErrandId("e").withBody("b").withAuthor("u").withCreated(ts);
		final var b = Note.create().withId("1").withErrandId("e").withBody("b").withAuthor("u").withCreated(ts);
		final var c = Note.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
