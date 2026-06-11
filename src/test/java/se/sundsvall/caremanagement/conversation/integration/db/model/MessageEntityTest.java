package se.sundsvall.caremanagement.conversation.integration.db.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var id = "m1";
		final var errandId = "e1";
		final var direction = "OUTBOUND";
		final var body = "body";
		final var author = "author";
		final var created = FIXED_TIMESTAMP;

		final var entity = MessageEntity.create()
			.withId(id)
			.withErrandId(errandId)
			.withDirection(direction)
			.withBody(body)
			.withAuthor(author)
			.withCreated(created);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getDirection()).isEqualTo(direction);
		assertThat(entity.getBody()).isEqualTo(body);
		assertThat(entity.getAuthor()).isEqualTo(author);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(MessageEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageEntity()).hasAllNullFieldsOrProperties();
	}
}
