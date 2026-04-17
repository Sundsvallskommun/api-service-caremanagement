package se.sundsvall.caremanagement.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.hibernate.LobHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import se.sundsvall.caremanagement.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandAttachmentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";
	private static final String ATTACHMENT_ID = "aid";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Captor
	private ArgumentCaptor<AttachmentEntity> attachmentCaptor;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandCaptor;

	@InjectMocks
	private ErrandAttachmentService service;

	@Test
	void createAttachment() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);
		final var file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
		final var lobHelper = mock(LobHelper.class);
		final var blob = mock(Blob.class);

		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(lobHelper.createBlob(any(), anyLong())).thenReturn(blob);
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class)))
			.thenAnswer(inv -> ((AttachmentEntity) inv.getArgument(0)).withId(ATTACHMENT_ID));

		try (final MockedStatic<Hibernate> hibernateMock = Mockito.mockStatic(Hibernate.class)) {
			hibernateMock.when(Hibernate::getLobHelper).thenReturn(lobHelper);

			final var result = service.createAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, file);

			assertThat(result).isEqualTo(ATTACHMENT_ID);
		}

		verify(attachmentRepositoryMock).save(attachmentCaptor.capture());
		assertThat(attachmentCaptor.getValue().getErrandEntity()).isSameAs(errand);
		assertThat(attachmentCaptor.getValue().getFileName()).isEqualTo("test.txt");
		assertThat(attachmentCaptor.getValue().getMimeType()).isEqualTo("text/plain");
	}

	@Test
	void createAttachment_errandNotFound() {
		final var file = new MockMultipartFile("file", "t.txt", "text/plain", "x".getBytes());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, file))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(attachmentRepositoryMock, never()).save(any());
	}

	@Test
	void readAttachments() {
		final var errand = ErrandEntity.create().withAttachments(new ArrayList<>(List.of(
			AttachmentEntity.create().withId("a"),
			AttachmentEntity.create().withId("b"))));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(2);
	}

	@Test
	void readAttachment() {
		final var attachment = AttachmentEntity.create().withId(ATTACHMENT_ID).withFileName("n.txt");
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachment));

		final var result = service.readAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getFileName()).isEqualTo("n.txt");
	}

	@Test
	void readAttachment_notFound() {
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void streamAttachmentFile() throws SQLException {
		final var blob = mock(Blob.class);
		final var attachment = AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName("file.txt")
			.withMimeType("text/plain")
			.withFileSize(7)
			.withAttachmentData(AttachmentDataEntity.create().withFile(blob));
		final var response = new MockHttpServletResponse();

		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachment));
		when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

		service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID, response);

		assertThat(response.getHeader(CONTENT_TYPE)).isEqualTo("text/plain");
		assertThat(response.getHeader(CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"file.txt\"");
		assertThat(response.getContentLength()).isEqualTo(7);
		assertThat(response.getContentAsByteArray()).isEqualTo("content".getBytes());
	}

	@Test
	void streamAttachmentFile_nullFileSize_skipsContentLength() throws SQLException, IOException {
		final var blob = mock(Blob.class);
		final var attachment = AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName("file.txt")
			.withMimeType("text/plain")
			.withAttachmentData(AttachmentDataEntity.create().withFile(blob));
		final var response = mock(HttpServletResponse.class);

		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachment));
		when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(response.getOutputStream()).thenReturn(new MockHttpServletResponse().getOutputStream());

		service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID, response);

		verify(response, never()).setContentLength(anyInt());
	}

	@Test
	void streamAttachmentFile_sqlException_throwsProblem() throws SQLException {
		final var blob = mock(Blob.class);
		final var attachment = AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName("file.txt")
			.withMimeType("text/plain")
			.withAttachmentData(AttachmentDataEntity.create().withFile(blob));
		final var response = new MockHttpServletResponse();

		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachment));
		when(blob.getBinaryStream()).thenThrow(new SQLException("boom"));

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID, response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);
	}

	@Test
	void streamAttachmentFile_ioException_throwsProblem() throws IOException {
		final var attachment = AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName("file.txt")
			.withMimeType("text/plain")
			.withAttachmentData(AttachmentDataEntity.create().withFile(mock(Blob.class)));
		final var response = mock(HttpServletResponse.class);

		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachment));
		when(response.getOutputStream()).thenThrow(new IOException("boom"));

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID, response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);
	}

	@Test
	void deleteAttachment() {
		final var attachment = AttachmentEntity.create().withId(ATTACHMENT_ID);
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withAttachments(new ArrayList<>(List.of(attachment)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.deleteAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID);

		assertThat(errand.getAttachments()).doesNotContain(attachment);
		verify(errandRepositoryMock).save(errandCaptor.capture());
		assertThat(errandCaptor.getValue()).isSameAs(errand);
	}

	@Test
	void deleteAttachment_attachmentNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withAttachments(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.deleteAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandRepositoryMock, never()).save(any(ErrandEntity.class));
	}
}
