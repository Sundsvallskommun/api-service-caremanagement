package se.sundsvall.caremanagement.conversation.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class MessageAttachmentEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(MessageAttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "attachment-1";
		final var messageId = "message-1";
		final var fileName = "certificate.pdf";
		final var mimeType = "application/pdf";
		final var fileSize = 1024;
		final var senderRole = "CLIENT";
		final var created = FIXED_TIMESTAMP;

		final var entity = MessageAttachmentEntity.create()
			.withId(id)
			.withMessageId(messageId)
			.withFileName(fileName)
			.withMimeType(mimeType)
			.withFileSize(fileSize)
			.withSenderRole(senderRole)
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMessageId()).isEqualTo(messageId);
		assertThat(entity.getFileName()).isEqualTo(fileName);
		assertThat(entity.getMimeType()).isEqualTo(mimeType);
		assertThat(entity.getFileSize()).isEqualTo(fileSize);
		assertThat(entity.getSenderRole()).isEqualTo(senderRole);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessageAttachmentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageAttachmentEntity()).hasAllNullFieldsOrProperties();
	}
}
