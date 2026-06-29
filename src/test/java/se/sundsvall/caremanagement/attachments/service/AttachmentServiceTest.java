package se.sundsvall.caremanagement.attachments.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.api.model.Attachment;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachment;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachmentContent;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachmentQueryService;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String ATTACHMENT_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";
	private static final String CLIENT_PDF_FILE_NAME = "klientbilagor.pdf";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private ConversationAttachmentQueryService conversationAttachmentQueryServiceMock;

	@InjectMocks
	private AttachmentService service;

	@Test
	void createAttachmentDefaultsOriginToErrand() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class)))
			.thenReturn(AttachmentEntity.create().withId(ATTACHMENT_ID));
		final var file = new MockMultipartFile("file", "f.pdf", "application/pdf", "x".getBytes());

		final var id = service.createAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, file);

		assertThat(id).isEqualTo(ATTACHMENT_ID);
		final ArgumentCaptor<AttachmentEntity> captor = ArgumentCaptor.forClass(AttachmentEntity.class);
		verify(attachmentRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getOrigin()).isEqualTo("ERRAND");
	}

	@Test
	void createAttachmentWithCaseDataRenamesFileToErrandNumberPdf() {
		final var errand = mock(ErrandEntity.class);
		when(errand.getErrandNumber()).thenReturn("EB-2024-000123");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.existsByErrandIdAndOrigin(ERRAND_ID, "CASE_DATA")).thenReturn(false);
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class)))
			.thenReturn(AttachmentEntity.create().withId(ATTACHMENT_ID));
		// A non-pdf source name proves the file is renamed to {errandNumber}.pdf regardless of what was uploaded.
		final var file = new MockMultipartFile("file", "whatever.png", "image/png", "x".getBytes());

		final var id = service.createAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CASE_DATA", file);

		assertThat(id).isEqualTo(ATTACHMENT_ID);
		final ArgumentCaptor<AttachmentEntity> captor = ArgumentCaptor.forClass(AttachmentEntity.class);
		verify(attachmentRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getOrigin()).isEqualTo("CASE_DATA");
		assertThat(captor.getValue().getFileName()).isEqualTo("EB-2024-000123.pdf");
	}

	@Test
	void createCaseDataAttachmentStoresAsCaseDataRenamedToErrandNumberPdf() {
		final var errand = mock(ErrandEntity.class);
		when(errand.getErrandNumber()).thenReturn("EB-2024-000999");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.existsByErrandIdAndOrigin(ERRAND_ID, "CASE_DATA")).thenReturn(false);
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class)))
			.thenReturn(AttachmentEntity.create().withId(ATTACHMENT_ID));
		final var file = new MockMultipartFile("caseData", "snapshot.pdf", "application/pdf", "x".getBytes());

		final var id = service.createCaseDataAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, file);

		assertThat(id).isEqualTo(ATTACHMENT_ID);
		final ArgumentCaptor<AttachmentEntity> captor = ArgumentCaptor.forClass(AttachmentEntity.class);
		verify(attachmentRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getOrigin()).isEqualTo("CASE_DATA");
		assertThat(captor.getValue().getFileName()).isEqualTo("EB-2024-000999.pdf");
	}

	@Test
	void createAttachmentWithCaseDataThrowsBadRequestWhenOneAlreadyExists() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.existsByErrandIdAndOrigin(ERRAND_ID, "CASE_DATA")).thenReturn(true);
		final var file = new MockMultipartFile("file", "arendeuppgifter.pdf", "application/pdf", "x".getBytes());

		assertThatThrownBy(() -> service.createAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CASE_DATA", file))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(attachmentRepositoryMock, never()).save(any(AttachmentEntity.class));
	}

	@Test
	void messageHistoryExistsDelegatesToRepository() {
		when(attachmentRepositoryMock.existsByErrandIdAndOrigin(ERRAND_ID, "MESSAGE_HISTORY")).thenReturn(true);

		assertThat(service.messageHistoryExists(ERRAND_ID)).isTrue();
		verify(attachmentRepositoryMock).existsByErrandIdAndOrigin(ERRAND_ID, "MESSAGE_HISTORY");
	}

	@Test
	void createMessageHistoryAttachmentStoresUnderGivenFileName() {
		final var fileName = "Meddelanden och bilagor från Draken_EB-2024-000777_2026-05-12--2026-05-20.pdf";
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.existsByErrandIdAndOrigin(ERRAND_ID, "MESSAGE_HISTORY")).thenReturn(false);
		when(attachmentRepositoryMock.save(any(AttachmentEntity.class)))
			.thenReturn(AttachmentEntity.create().withId(ATTACHMENT_ID));

		final var id = service.createMessageHistoryAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, fileName, "%PDF".getBytes());

		assertThat(id).isEqualTo(ATTACHMENT_ID);
		final ArgumentCaptor<AttachmentEntity> captor = ArgumentCaptor.forClass(AttachmentEntity.class);
		verify(attachmentRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getOrigin()).isEqualTo("MESSAGE_HISTORY");
		assertThat(captor.getValue().getFileName()).isEqualTo(fileName);
		assertThat(captor.getValue().getMimeType()).isEqualTo("application/pdf");
	}

	@Test
	void createMessageHistoryAttachmentThrowsBadRequestWhenOneAlreadyExists() {
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.existsByErrandIdAndOrigin(ERRAND_ID, "MESSAGE_HISTORY")).thenReturn(true);

		assertThatThrownBy(() -> service.createMessageHistoryAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "name.pdf", "%PDF".getBytes()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(attachmentRepositoryMock, never()).save(any(AttachmentEntity.class));
	}

	@Test
	void combineToPdfMergesSourcesIntoAPdf() {
		final var result = service.combineToPdf(List.of(
			new CombineSource("meddelanden.pdf", "application/pdf", "%PDF-1.4 minimal".getBytes()),
			new CombineSource("note.txt", "text/plain", "en anteckning".getBytes())));

		assertThat(result).isNotEmpty();
		assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
	}

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

	@Test
	void readAttachmentsMergesErrandAndConversationAttachmentsSortedByCreated() {
		final var t1 = OffsetDateTime.parse("2024-01-01T10:00:00Z");
		final var t2 = OffsetDateTime.parse("2024-01-01T11:00:00Z");
		final var t3 = OffsetDateTime.parse("2024-01-01T12:00:00Z");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			AttachmentEntity.create().withId("app").withFileName("ansokan.pdf").withOrigin("APPLICATION").withSenderRole("CLIENT").withCreated(t1),
			AttachmentEntity.create().withId("gen").withFileName("sammanstallning.pdf").withOrigin("GENERATED").withSenderRole("CLIENT").withCreated(t3)));
		when(conversationAttachmentQueryServiceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(
			new ConversationAttachment("conv", "msg-1", "intyg.pdf", "application/pdf", 10, t2, "CLIENT")));

		final var result = service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null);

		assertThat(result).extracting(Attachment::getId).containsExactly("app", "conv", "gen");
		assertThat(result).extracting(Attachment::getOrigin).containsExactly("APPLICATION", "CONVERSATION", "GENERATED");
		assertThat(result.get(1).getMessageId()).isEqualTo("msg-1");
		assertThat(result.get(1).getSenderRole()).isEqualTo("CLIENT");
	}

	@Test
	void readAttachmentsWithoutConversationAttachmentsReturnsOnlyErrandAttachments() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			AttachmentEntity.create().withId("app").withOrigin("APPLICATION").withCreated(OffsetDateTime.parse("2024-01-01T10:00:00Z"))));
		when(conversationAttachmentQueryServiceMock.listForErrand(ERRAND_ID)).thenReturn(List.of());

		final var result = service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null);

		assertThat(result).extracting(Attachment::getId).containsExactly("app");
	}

	@Test
	void readAttachmentsSortsNullCreatedLast() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			AttachmentEntity.create().withId("no-date"),
			AttachmentEntity.create().withId("dated").withCreated(OffsetDateTime.parse("2024-01-01T10:00:00Z"))));
		when(conversationAttachmentQueryServiceMock.listForErrand(ERRAND_ID)).thenReturn(List.of());

		final var result = service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null);

		assertThat(result).extracting(Attachment::getId).containsExactly("dated", "no-date");
	}

	@Test
	void readAttachmentsFilteredByOriginReturnsOnlyMatching() {
		final var t1 = OffsetDateTime.parse("2024-01-01T10:00:00Z");
		final var t2 = OffsetDateTime.parse("2024-01-01T11:00:00Z");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			AttachmentEntity.create().withId("app").withOrigin("APPLICATION").withSenderRole("CLIENT").withCreated(t1)));
		when(conversationAttachmentQueryServiceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(
			new ConversationAttachment("conv", "msg-1", "intyg.pdf", "application/pdf", 10, t2, "CLIENT")));

		final var result = service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CONVERSATION", null);

		assertThat(result).extracting(Attachment::getId).containsExactly("conv");
		assertThat(result).extracting(Attachment::getOrigin).containsOnly("CONVERSATION");
	}

	@Test
	void readAttachmentsFilteredBySenderRoleReturnsOnlyMatching() {
		final var t1 = OffsetDateTime.parse("2024-01-01T10:00:00Z");
		final var t2 = OffsetDateTime.parse("2024-01-01T11:00:00Z");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(mock(ErrandEntity.class)));
		when(attachmentRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			AttachmentEntity.create().withId("upload").withOrigin("ERRAND").withSenderRole("CASEWORKER").withCreated(t1)));
		when(conversationAttachmentQueryServiceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(
			new ConversationAttachment("conv", "msg-1", "intyg.pdf", "application/pdf", 10, t2, "CLIENT")));

		final var result = service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, "CLIENT");

		assertThat(result).extracting(Attachment::getId).containsExactly("conv");
		assertThat(result).extracting(Attachment::getSenderRole).containsOnly("CLIENT");
	}

	@Test
	void readAttachmentsErrandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readAttachments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(conversationAttachmentQueryServiceMock);
	}

	@Test
	void regenerateClientAttachmentPdfInsertsWhenAbsent() {
		when(conversationAttachmentQueryServiceMock.clientAttachmentContentsForErrand(ERRAND_ID)).thenReturn(List.of(
			new ConversationAttachmentContent("intyg.pdf", "application/pdf", "%PDF-1.4 minimal".getBytes())));
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.findFirstByErrandIdAndFileNameAndOrigin(ERRAND_ID, CLIENT_PDF_FILE_NAME, "CONVERSATION"))
			.thenReturn(Optional.empty());

		service.regenerateClientAttachmentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		final ArgumentCaptor<AttachmentEntity> captor = ArgumentCaptor.forClass(AttachmentEntity.class);
		verify(attachmentRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getFileName()).isEqualTo(CLIENT_PDF_FILE_NAME);
		assertThat(captor.getValue().getOrigin()).isEqualTo("CONVERSATION");
		assertThat(captor.getValue().getSenderRole()).isEqualTo("CLIENT");
		assertThat(captor.getValue().getMimeType()).isEqualTo("application/pdf");
		assertThat(captor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
	}

	@Test
	void regenerateClientAttachmentPdfUpdatesInPlaceWhenPresent() {
		final var originalBlob = mock(Blob.class);
		final var existing = AttachmentEntity.create()
			.withId("existing-id").withErrandId(ERRAND_ID).withFileName(CLIENT_PDF_FILE_NAME).withOrigin("CONVERSATION")
			.withAttachmentData(AttachmentDataEntity.create().withFile(originalBlob));
		when(conversationAttachmentQueryServiceMock.clientAttachmentContentsForErrand(ERRAND_ID)).thenReturn(List.of(
			new ConversationAttachmentContent("intyg.pdf", "application/pdf", "%PDF-1.4 minimal".getBytes())));
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(attachmentRepositoryMock.findFirstByErrandIdAndFileNameAndOrigin(ERRAND_ID, CLIENT_PDF_FILE_NAME, "CONVERSATION"))
			.thenReturn(Optional.of(existing));

		service.regenerateClientAttachmentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(attachmentRepositoryMock).save(existing);
		// Same row (id preserved), with refreshed content.
		assertThat(existing.getId()).isEqualTo("existing-id");
		assertThat(existing.getFileSize()).isNotNull().isPositive();
		assertThat(existing.getAttachmentData().getFile()).isNotSameAs(originalBlob);
	}

	@Test
	void regenerateClientAttachmentPdfIsNoOpWhenNoClientAttachments() {
		when(conversationAttachmentQueryServiceMock.clientAttachmentContentsForErrand(ERRAND_ID)).thenReturn(List.of());

		service.regenerateClientAttachmentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verifyNoInteractions(attachmentRepositoryMock);
	}
}
