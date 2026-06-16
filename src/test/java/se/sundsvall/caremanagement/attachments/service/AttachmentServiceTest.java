package se.sundsvall.caremanagement.attachments.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String ATTACHMENT_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@InjectMocks
	private AttachmentService service;

	@Test
	void storeAndCombinePersistsEachSourceAndACombinedPdf() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class)))
			.thenReturn(AttachmentEntity.create().withId(ATTACHMENT_ID));

		final List<MultipartFile> files = List.of(
			new MockMultipartFile("attachments", "hyreskontrakt.pdf", "application/pdf", "%PDF-1.4 minimal".getBytes()),
			new MockMultipartFile("attachments", "hyresavi.png", "image/png", new byte[] {
				9, 8, 7
			}));

		final var ids = service.storeAndCombine(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, files);

		// Two source attachments + one generated combined PDF.
		assertThat(ids).hasSize(3);
		verify(attachmentRepositoryMock, times(3)).save(any(AttachmentEntity.class));
	}

	@Test
	void storeAndCombineWrapsUnreadableFileAsBadRequest() throws IOException {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));

		final var unreadable = mock(MultipartFile.class);
		when(unreadable.getBytes()).thenThrow(new IOException("disk gone"));

		assertThatThrownBy(() -> service.storeAndCombine(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, List.of(unreadable)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void streamAttachmentFileWrapsSqlExceptionAsProblem() throws SQLException {
		// Force the IOException | SQLException catch branch via a blob whose stream throws SQLException.
		final var blob = mock(Blob.class);
		when(blob.getBinaryStream()).thenThrow(new SQLException("blob boom"));
		final var data = AttachmentDataEntity.create().withFile(blob);
		final var attachment = AttachmentEntity.create()
			.withId(ATTACHMENT_ID).withErrandId(ERRAND_ID)
			.withFileName("f.txt").withMimeType("text/plain").withFileSize(10)
			.withAttachmentData(data);
		final var response = mock(HttpServletResponse.class);

		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachment));

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID, response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);
	}
}
