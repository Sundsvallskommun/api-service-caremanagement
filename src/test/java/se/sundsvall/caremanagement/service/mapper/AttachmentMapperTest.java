package se.sundsvall.caremanagement.service.mapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LobHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentMapperTest {

	@Mock
	private MultipartFile multipartFileMock;

	@Mock
	private LobHelper lobHelperMock;

	@Mock
	private Blob blobMock;

	@Test
	void toAttachment_maps() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = AttachmentEntity.create()
			.withId("aid")
			.withFileName("file.pdf")
			.withMimeType("application/pdf")
			.withFileSize(1024)
			.withCreated(created)
			.withModified(modified);

		final var result = AttachmentMapper.toAttachment(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("aid");
		assertThat(result.getFileName()).isEqualTo("file.pdf");
		assertThat(result.getMimeType()).isEqualTo("application/pdf");
		assertThat(result.getFileSize()).isEqualTo(1024);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void toAttachment_nullReturnsNull() {
		assertThat(AttachmentMapper.toAttachment(null)).isNull();
	}

	@Test
	void toAttachmentEntity_maps() throws IOException {
		final var errand = ErrandEntity.create().withId("eid").withNamespace("ns").withMunicipalityId("2281");

		when(multipartFileMock.getOriginalFilename()).thenReturn("file.pdf");
		when(multipartFileMock.getContentType()).thenReturn("application/pdf");
		when(multipartFileMock.getSize()).thenReturn(4L);
		when(multipartFileMock.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		try (MockedStatic<Hibernate> hibernateMock = Mockito.mockStatic(Hibernate.class)) {
			hibernateMock.when(Hibernate::getLobHelper).thenReturn(lobHelperMock);
			when(lobHelperMock.createBlob(any(), anyLong())).thenReturn(blobMock);

			final var result = AttachmentMapper.toAttachmentEntity(errand, multipartFileMock);

			assertThat(result).isNotNull();
			assertThat(result.getErrandEntity()).isSameAs(errand);
			assertThat(result.getNamespace()).isEqualTo("ns");
			assertThat(result.getMunicipalityId()).isEqualTo("2281");
			assertThat(result.getFileName()).isEqualTo("file.pdf");
			assertThat(result.getMimeType()).isEqualTo("application/pdf");
			assertThat(result.getFileSize()).isEqualTo(4);
			assertThat(result.getAttachmentData()).isNotNull();
			assertThat(result.getAttachmentData().getFile()).isSameAs(blobMock);
		}
	}

	@Test
	void toAttachmentEntity_ioException_throwsProblem() throws IOException {
		final var errand = ErrandEntity.create().withId("eid").withNamespace("ns").withMunicipalityId("2281");

		when(multipartFileMock.getInputStream()).thenThrow(new IOException("boom"));

		assertThatThrownBy(() -> AttachmentMapper.toAttachmentEntity(errand, multipartFileMock))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Could not read input stream")
			.hasMessageContaining("boom");
	}

	@Test
	void toAttachmentEntity_nullErrandReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity(null, multipartFileMock)).isNull();
	}

	@Test
	void toAttachmentEntity_nullFileReturnsNull() {
		assertThat(AttachmentMapper.toAttachmentEntity(ErrandEntity.create(), null)).isNull();
	}

	@Test
	void toAttachmentList_maps() {
		final var result = AttachmentMapper.toAttachmentList(List.of(
			AttachmentEntity.create().withId("a").withAttachmentData(AttachmentDataEntity.create()),
			AttachmentEntity.create().withId("b").withAttachmentData(AttachmentDataEntity.create())));

		assertThat(result).hasSize(2);
	}

	@Test
	void toAttachmentList_nullReturnsEmpty() {
		assertThat(AttachmentMapper.toAttachmentList(null)).isEmpty();
	}
}
