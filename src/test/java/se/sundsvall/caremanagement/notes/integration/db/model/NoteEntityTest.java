package se.sundsvall.caremanagement.notes.integration.db.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var id = "n1";
		final var errandId = "e1";
		final var body = "body";
		final var author = "author";
		final var created = FIXED_TIMESTAMP;
		final var modifiedBy = "editor";
		final var modified = FIXED_TIMESTAMP.plusHours(1);

		final var entity = NoteEntity.create()
			.withId(id)
			.withErrandId(errandId)
			.withBody(body)
			.withAuthor(author)
			.withCreated(created)
			.withModifiedBy(modifiedBy)
			.withModified(modified);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getBody()).isEqualTo(body);
		assertThat(entity.getAuthor()).isEqualTo(author);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModifiedBy()).isEqualTo(modifiedBy);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(NoteEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new NoteEntity()).hasAllNullFieldsOrProperties();
	}
}
