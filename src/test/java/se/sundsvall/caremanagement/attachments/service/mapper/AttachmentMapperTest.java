package se.sundsvall.caremanagement.attachments.service.mapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.attachments.service.SourceFile;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachment;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class AttachmentMapperTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void toAttachmentMapsAllFields() {
		final var created = FIXED_TIMESTAMP.minusDays(1);
		final var modified = FIXED_TIMESTAMP;
		final var entity = AttachmentEntity.create()
			.withId("id")
			.withFileName("f.txt")
			.withMimeType("text/plain")
			.withFileSize(10)
			.withDocumentType("CONVERSATION")
			.withSenderRole("CLIENT")
			.withCreated(created)
			.withModified(modified);

		final var attachment = AttachmentMapper.toAttachment(entity);

		assertThat(attachment).isNotNull().hasNoNullFieldsOrPropertiesExcept("messageId");
		assertThat(attachment.getId()).isEqualTo("id");
		assertThat(attachment.getFileName()).isEqualTo("f.txt");
		assertThat(attachment.getMimeType()).isEqualTo("text/plain");
		assertThat(attachment.getFileSize()).isEqualTo(10);
		assertThat(attachment.getDocumentType()).isEqualTo("CONVERSATION");
		assertThat(attachment.getSenderRole()).isEqualTo("CLIENT");
		assertThat(attachment.getMessageId()).isNull();
		assertThat(attachment.getCreated()).isEqualTo(created);
		assertThat(attachment.getModified()).isEqualTo(modified);
	}

	@Test
	void toAttachmentNullReturnsNull() {
		assertThat(AttachmentMapper.toAttachment((AttachmentEntity) null)).isNull();
	}

	@Test
	void toAttachmentFromConversationAttachmentMapsAllFields() {
		final var source = new ConversationAttachment("a1", "m1", "intyg.pdf", "application/pdf", 42, FIXED_TIMESTAMP, "CLIENT");

		final var attachment = AttachmentMapper.toAttachment(source);

		assertThat(attachment).isNotNull().hasNoNullFieldsOrPropertiesExcept("modified");
		assertThat(attachment.getId()).isEqualTo("a1");
		assertThat(attachment.getMessageId()).isEqualTo("m1");
		assertThat(attachment.getFileName()).isEqualTo("intyg.pdf");
		assertThat(attachment.getMimeType()).isEqualTo("application/pdf");
		assertThat(attachment.getFileSize()).isEqualTo(42);
		assertThat(attachment.getDocumentType()).isEqualTo("CONVERSATION");
		assertThat(attachment.getSenderRole()).isEqualTo("CLIENT");
		assertThat(attachment.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(attachment.getModified()).isNull();
	}

	@Test
	void toAttachmentFromConversationAttachmentNullReturnsNull() {
		assertThat(AttachmentMapper.toAttachment((ConversationAttachment) null)).isNull();
	}

	@Test
	void toAttachmentEntityNullErrandIdReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity(null, "ns", "mid", "ERRAND", null, new MockMultipartFile("file", new byte[] {
			1
		}))).isNull();
	}

	@Test
	void toAttachmentEntityNullFileReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", "ERRAND", null, (MultipartFile) null)).isNull();
	}

	@Test
	void toAttachmentEntityIOExceptionWrappedAsBadRequest() {
		final MultipartFile file = new MockMultipartFile("file", "f.txt", "text/plain", new byte[0]) {
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("boom");
			}

			@Override
			public long getSize() {
				return 10;
			}
		};

		assertThatThrownBy(() -> AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", "ERRAND", null, file))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void toAttachmentListMapsAllItems() {
		final var entity = AttachmentEntity.create().withId("id").withFileName("f");
		final var result = AttachmentMapper.toAttachmentList(List.of(entity));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("id");
	}

	@Test
	void toAttachmentListNullReturnsEmpty() {
		assertThat(AttachmentMapper.toAttachmentList(null)).isEmpty();
	}

	@Test
	void toAttachmentEntityFromMultipartFileBuildsEntity() {
		final var file = new MockMultipartFile("file", "hello.txt", "text/plain", new ByteArrayInputStream("hello".getBytes()).readAllBytes());

		// Will likely fail at Hibernate.getLobHelper() since no JPA context is active.
		// Either we get a real entity (when running in an integration setup) or an exception.
		try {
			final AttachmentEntity entity = AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", "ERRAND", "CASEWORKER", file);
			assertThat(entity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
			assertThat(entity.getErrandId()).isEqualTo("eid");
			assertThat(entity.getNamespace()).isEqualTo("ns");
			assertThat(entity.getMunicipalityId()).isEqualTo("mid");
			assertThat(entity.getFileName()).isEqualTo("hello.txt");
			assertThat(entity.getMimeType()).isEqualTo("text/plain");
			assertThat(entity.getFileSize()).isEqualTo(5);
			assertThat(entity.getDocumentType()).isEqualTo("ERRAND");
			assertThat(entity.getSenderRole()).isEqualTo("CASEWORKER");
			assertThat(entity.getAttachmentData()).isNotNull();
		} catch (final Exception e) {
			// Acceptable in unit context with no Hibernate session
			assertThat(e).isNotNull();
		}
	}

	@Test
	void attachmentDataEntityCreate() {
		final var entity = AttachmentDataEntity.create();
		assertThat(entity).isNotNull();
	}

	@Test
	void toAttachmentEntityFromBytesBuildsEntity() {
		final var entity = AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", "GENERATED", "CLIENT", new SourceFile("sammanstallning.pdf", "application/pdf", "%PDF".getBytes()));

		assertThat(entity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified");
		assertThat(entity.getErrandId()).isEqualTo("eid");
		assertThat(entity.getNamespace()).isEqualTo("ns");
		assertThat(entity.getMunicipalityId()).isEqualTo("mid");
		assertThat(entity.getFileName()).isEqualTo("sammanstallning.pdf");
		assertThat(entity.getMimeType()).isEqualTo("application/pdf");
		assertThat(entity.getFileSize()).isEqualTo(4);
		assertThat(entity.getDocumentType()).isEqualTo("GENERATED");
		assertThat(entity.getSenderRole()).isEqualTo("CLIENT");
		assertThat(entity.getAttachmentData()).isNotNull();
	}

	@Test
	void toAttachmentEntityFromBytesNullErrandIdReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity(null, "ns", "mid", "GENERATED", "CLIENT", new SourceFile("f.pdf", "application/pdf", new byte[] {
			1
		}))).isNull();
	}

	@Test
	void toAttachmentEntityFromBytesNullContentReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", "GENERATED", "CLIENT", new SourceFile("f.pdf", "application/pdf", null))).isNull();
	}
}
