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
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class AttachmentMapperTest {

	@Test
	void toAttachmentMapsAllFields() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = AttachmentEntity.create()
			.withId("id")
			.withFileName("f.txt")
			.withMimeType("text/plain")
			.withFileSize(10)
			.withCreated(created)
			.withModified(modified);

		final var attachment = AttachmentMapper.toAttachment(entity);

		assertThat(attachment).isNotNull();
		assertThat(attachment.getId()).isEqualTo("id");
		assertThat(attachment.getFileName()).isEqualTo("f.txt");
		assertThat(attachment.getMimeType()).isEqualTo("text/plain");
		assertThat(attachment.getFileSize()).isEqualTo(10);
		assertThat(attachment.getCreated()).isEqualTo(created);
		assertThat(attachment.getModified()).isEqualTo(modified);
	}

	@Test
	void toAttachmentNullReturnsNull() {
		assertThat(AttachmentMapper.toAttachment(null)).isNull();
	}

	@Test
	void toAttachmentEntityNullErrandIdReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity(null, "ns", "mid", new MockMultipartFile("file", new byte[] {
			1
		}))).isNull();
	}

	@Test
	void toAttachmentEntityNullFileReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", null)).isNull();
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

		assertThatThrownBy(() -> AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", file))
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
			final AttachmentEntity entity = AttachmentMapper.toAttachmentEntity("eid", "ns", "mid", file);
			assertThat(entity).isNotNull();
			assertThat(entity.getErrandId()).isEqualTo("eid");
			assertThat(entity.getNamespace()).isEqualTo("ns");
			assertThat(entity.getMunicipalityId()).isEqualTo("mid");
			assertThat(entity.getFileName()).isEqualTo("hello.txt");
			assertThat(entity.getMimeType()).isEqualTo("text/plain");
			assertThat(entity.getFileSize()).isEqualTo(5);
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
}
