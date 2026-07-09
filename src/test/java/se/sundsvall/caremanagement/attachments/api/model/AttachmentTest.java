package se.sundsvall.caremanagement.attachments.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
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
import static org.hamcrest.MatcherAssert.assertThat;

class AttachmentTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Attachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var fileName = "file.pdf";
		final var mimeType = "application/pdf";
		final var fileSize = 1024;
		final var documentType = "CONVERSATION";
		final var senderRole = "CLIENT";
		final var messageId = "message-1";
		final var created = FIXED_TIMESTAMP;
		final var modified = FIXED_TIMESTAMP;

		final var result = Attachment.create()
			.withId(id)
			.withFileName(fileName)
			.withMimeType(mimeType)
			.withFileSize(fileSize)
			.withDocumentType(documentType)
			.withSenderRole(senderRole)
			.withMessageId(messageId)
			.withCreated(created)
			.withModified(modified);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getFileName()).isEqualTo(fileName);
		assertThat(result.getMimeType()).isEqualTo(mimeType);
		assertThat(result.getFileSize()).isEqualTo(fileSize);
		assertThat(result.getDocumentType()).isEqualTo(documentType);
		assertThat(result.getSenderRole()).isEqualTo(senderRole);
		assertThat(result.getMessageId()).isEqualTo(messageId);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Attachment.create()).hasAllNullFieldsOrProperties();
	}
}
