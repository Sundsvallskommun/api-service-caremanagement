package se.sundsvall.caremanagement.conversation.service.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class MessageMapperTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void toMessageMapsAllFieldsAndAttachments() {
		final var entity = MessageEntity.create()
			.withId("m1")
			.withErrandId("e1")
			.withDirection("OUTBOUND")
			.withBody("body")
			.withAuthor("author")
			.withInReplyToId("r1")
			.withCreated(FIXED_TIMESTAMP);
		final var attachment = MessageAttachmentEntity.create().withId("a1").withMessageId("m1").withFileName("f.pdf").withMimeType("application/pdf").withFileSize(4).withSenderRole("CASEWORKER");

		final var message = MessageMapper.toMessage(entity, List.of(attachment));

		assertThat(message).isNotNull();
		assertThat(message.getId()).isEqualTo("m1");
		assertThat(message.getErrandId()).isEqualTo("e1");
		assertThat(message.getDirection()).isEqualTo("OUTBOUND");
		assertThat(message.getBody()).isEqualTo("body");
		assertThat(message.getAuthor()).isEqualTo("author");
		assertThat(message.getInReplyToId()).isEqualTo("r1");
		assertThat(message.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(message.getAttachments()).hasSize(1);
		assertThat(message.getAttachments().getFirst().getId()).isEqualTo("a1");
		assertThat(message.getAttachments().getFirst().getFileName()).isEqualTo("f.pdf");
		assertThat(message.getAttachments().getFirst().getSenderRole()).isEqualTo("CASEWORKER");
	}

	@Test
	void toMessageWithoutAttachmentsYieldsEmptyList() {
		final var message = MessageMapper.toMessage(MessageEntity.create().withId("m1"), emptyList());

		assertThat(message).isNotNull();
		assertThat(message.getAttachments()).isEmpty();
	}

	@Test
	void toMessageNullReturnsNull() {
		assertThat(MessageMapper.toMessage(null, emptyList())).isNull();
	}

	@Test
	void toMessageAttachmentMapsAllFields() {
		final var entity = MessageAttachmentEntity.create().withId("a1").withFileName("f.pdf").withMimeType("application/pdf").withFileSize(4).withSenderRole("CLIENT").withCreated(FIXED_TIMESTAMP);

		final var attachment = MessageMapper.toMessageAttachment(entity);

		assertThat(attachment).isNotNull();
		assertThat(attachment.getId()).isEqualTo("a1");
		assertThat(attachment.getFileName()).isEqualTo("f.pdf");
		assertThat(attachment.getMimeType()).isEqualTo("application/pdf");
		assertThat(attachment.getFileSize()).isEqualTo(4);
		assertThat(attachment.getSenderRole()).isEqualTo("CLIENT");
		assertThat(attachment.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void toMessageAttachmentNullReturnsNull() {
		assertThat(MessageMapper.toMessageAttachment(null)).isNull();
	}

	@Test
	void toMessageAttachmentListMapsAllItems() {
		final var result = MessageMapper.toMessageAttachmentList(List.of(MessageAttachmentEntity.create().withId("a1")));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("a1");
	}

	@Test
	void toMessageAttachmentListNullReturnsEmpty() {
		assertThat(MessageMapper.toMessageAttachmentList(null)).isEmpty();
	}

	@Test
	void toMessageAttachmentEntityInboundBuildsClientMetadata() {
		final var file = new MockMultipartFile("attachments", "hello.txt", "text/plain", "hello".getBytes());

		final var entity = MessageMapper.toMessageAttachmentEntity("m1", "INBOUND", file);

		assertThat(entity).isNotNull();
		assertThat(entity.getMessageId()).isEqualTo("m1");
		assertThat(entity.getFileName()).isEqualTo("hello.txt");
		assertThat(entity.getMimeType()).isEqualTo("text/plain");
		assertThat(entity.getFileSize()).isEqualTo(5);
		assertThat(entity.getSenderRole()).isEqualTo("CLIENT");
		assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	void toMessageAttachmentEntityOutboundBuildsHandlaggareMetadata() {
		final var file = new MockMultipartFile("attachments", "beslut.pdf", "application/pdf", "x".getBytes());

		final var entity = MessageMapper.toMessageAttachmentEntity("m1", "OUTBOUND", file);

		assertThat(entity).isNotNull();
		assertThat(entity.getSenderRole()).isEqualTo("CASEWORKER");
	}

	@Test
	void toMessageAttachmentEntityNullMessageIdReturnsNull() {
		assertThat(MessageMapper.toMessageAttachmentEntity(null, "INBOUND", new MockMultipartFile("attachments", new byte[] {
			1
		}))).isNull();
	}

	@Test
	void toMessageAttachmentEntityNullFileReturnsNull() {
		assertThat(MessageMapper.toMessageAttachmentEntity("m1", "INBOUND", null)).isNull();
	}

	@Test
	void toMessageAttachmentDataEntityBuildsBlob() {
		final var file = new MockMultipartFile("attachments", "hello.txt", "text/plain", "hello".getBytes());

		final var entity = MessageMapper.toMessageAttachmentDataEntity("a1", file);

		assertThat(entity).isNotNull();
		assertThat(entity.getMessageAttachmentId()).isEqualTo("a1");
		assertThat(entity.getFile()).isNotNull();
	}

	@Test
	void toMessageAttachmentDataEntityNullAttachmentIdReturnsNull() {
		assertThat(MessageMapper.toMessageAttachmentDataEntity(null, new MockMultipartFile("attachments", new byte[] {
			1
		}))).isNull();
	}

	@Test
	void toMessageAttachmentDataEntityNullFileReturnsNull() {
		assertThat(MessageMapper.toMessageAttachmentDataEntity("a1", null)).isNull();
	}

	@Test
	void toMessageAttachmentDataEntityIOExceptionWrappedAsBadRequest() {
		final MultipartFile file = new MockMultipartFile("attachments", "f.txt", "text/plain", new byte[0]) {
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("boom");
			}

			@Override
			public long getSize() {
				return 10;
			}
		};

		assertThatThrownBy(() -> MessageMapper.toMessageAttachmentDataEntity("a1", file))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}
}
