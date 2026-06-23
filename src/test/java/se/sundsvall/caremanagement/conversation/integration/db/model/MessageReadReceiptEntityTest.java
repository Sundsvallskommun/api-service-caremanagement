package se.sundsvall.caremanagement.conversation.integration.db.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageReadReceiptEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var id = "r1";
		final var messageId = "m1";
		final var readerSide = "CASEWORKER";
		final var readBy = "joe001doe";
		final var readAt = FIXED_TIMESTAMP;

		final var entity = MessageReadReceiptEntity.create()
			.withId(id)
			.withMessageId(messageId)
			.withReaderSide(readerSide)
			.withReadBy(readBy)
			.withReadAt(readAt);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMessageId()).isEqualTo(messageId);
		assertThat(entity.getReaderSide()).isEqualTo(readerSide);
		assertThat(entity.getReadBy()).isEqualTo(readBy);
		assertThat(entity.getReadAt()).isEqualTo(readAt);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(MessageReadReceiptEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageReadReceiptEntity()).hasAllNullFieldsOrProperties();
	}
}
