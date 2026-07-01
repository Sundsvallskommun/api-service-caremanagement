package se.sundsvall.caremanagement.conversation.service;

import java.sql.Blob;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mariadb.jdbc.MariaDbBlob;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentDataRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentDataEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.service.event.MessagePosted;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@Mock
	private MessageRepository repositoryMock;

	@Mock
	private MessageAttachmentRepository attachmentRepositoryMock;

	@Mock
	private MessageAttachmentDataRepository attachmentDataRepositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@InjectMocks
	private MessageService service;

	private void errandMissing(final String errandId) {
		doThrow(Problem.valueOf(NOT_FOUND, "No errand"))
			.when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, errandId);
	}

	@Test
	void postWithoutAttachmentsPublishesEventAndReturnsId() {
		final var saved = MessageEntity.create().withId("message-1").withErrandId("errand-1").withDirection("OUTBOUND").withAuthor("author").withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.save(any(MessageEntity.class))).thenReturn(saved);

		final var id = service.post(MUNICIPALITY_ID, NAMESPACE, "errand-1", new CreateMessage("OUTBOUND", "body", "author", null), null);

		assertThat(id).isEqualTo("message-1");

		final ArgumentCaptor<MessageEntity> entityCaptor = ArgumentCaptor.forClass(MessageEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo("errand-1");
		assertThat(entityCaptor.getValue().getDirection()).isEqualTo("OUTBOUND");
		assertThat(entityCaptor.getValue().getBody()).isEqualTo("body");
		assertThat(entityCaptor.getValue().getAuthor()).isEqualTo("author");
		assertThat(entityCaptor.getValue().getInReplyToId()).isNull();
		assertThat(entityCaptor.getValue().getCreated()).isNotNull();

		verifyNoInteractions(attachmentRepositoryMock, attachmentDataRepositoryMock);

		final ArgumentCaptor<MessagePosted> eventCaptor = ArgumentCaptor.forClass(MessagePosted.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().messageId()).isEqualTo("message-1");
		assertThat(eventCaptor.getValue().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(eventCaptor.getValue().namespace()).isEqualTo(NAMESPACE);
		assertThat(eventCaptor.getValue().errandId()).isEqualTo("errand-1");
		assertThat(eventCaptor.getValue().direction()).isEqualTo("OUTBOUND");
		assertThat(eventCaptor.getValue().author()).isEqualTo("author");
		assertThat(eventCaptor.getValue().hasAttachments()).isFalse();
	}

	@Test
	void postOnUnknownErrandIsNotFoundAndPersistsNothing() {
		errandMissing("errand-1");

		assertThatThrownBy(() -> service.post(MUNICIPALITY_ID, NAMESPACE, "errand-1", new CreateMessage("OUTBOUND", "body", "author", null), null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
		verifyNoInteractions(eventsMock);
	}

	@Test
	void postWithAttachmentsStoresEachAttachment() {
		final var saved = MessageEntity.create().withId("message-1").withErrandId("errand-1").withDirection("INBOUND").withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.save(any(MessageEntity.class))).thenReturn(saved);
		when(attachmentRepositoryMock.save(any(MessageAttachmentEntity.class)))
			.thenReturn(MessageAttachmentEntity.create().withId("attachment-1"));

		final var file = new MockMultipartFile("attachments", "certificate.pdf", "application/pdf", "%PDF".getBytes());

		final var id = service.post(MUNICIPALITY_ID, NAMESPACE, "errand-1", new CreateMessage("INBOUND", "body", null, null), List.of(file));

		assertThat(id).isEqualTo("message-1");

		final ArgumentCaptor<MessageAttachmentEntity> attachmentCaptor = ArgumentCaptor.forClass(MessageAttachmentEntity.class);
		verify(attachmentRepositoryMock).save(attachmentCaptor.capture());
		assertThat(attachmentCaptor.getValue().getMessageId()).isEqualTo("message-1");
		assertThat(attachmentCaptor.getValue().getFileName()).isEqualTo("certificate.pdf");
		assertThat(attachmentCaptor.getValue().getMimeType()).isEqualTo("application/pdf");
		assertThat(attachmentCaptor.getValue().getFileSize()).isEqualTo(4);
		assertThat(attachmentCaptor.getValue().getSenderRole()).isEqualTo("CLIENT");

		final ArgumentCaptor<MessageAttachmentDataEntity> dataCaptor = ArgumentCaptor.forClass(MessageAttachmentDataEntity.class);
		verify(attachmentDataRepositoryMock).save(dataCaptor.capture());
		assertThat(dataCaptor.getValue().getMessageAttachmentId()).isEqualTo("attachment-1");
		assertThat(dataCaptor.getValue().getFile()).isNotNull();

		final ArgumentCaptor<MessagePosted> eventCaptor = ArgumentCaptor.forClass(MessagePosted.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().hasAttachments()).isTrue();
		assertThat(eventCaptor.getValue().direction()).isEqualTo("INBOUND");
	}

	@Test
	void postWithInReplyToValidatesAndPersistsTheReference() {
		when(repositoryMock.findByIdAndErrandId("parent-1", "errand-1"))
			.thenReturn(Optional.of(MessageEntity.create().withId("parent-1").withErrandId("errand-1")));
		when(repositoryMock.save(any(MessageEntity.class))).thenReturn(MessageEntity.create().withId("message-1"));

		final var id = service.post(MUNICIPALITY_ID, NAMESPACE, "errand-1", new CreateMessage("OUTBOUND", "body", "author", "parent-1"), null);

		assertThat(id).isEqualTo("message-1");

		final ArgumentCaptor<MessageEntity> entityCaptor = ArgumentCaptor.forClass(MessageEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getInReplyToId()).isEqualTo("parent-1");

		verify(eventsMock).publishEvent(any(MessagePosted.class));
	}

	@Test
	void postWithUnknownInReplyToIsBadRequestAndPersistsNothing() {
		when(repositoryMock.findByIdAndErrandId("missing-parent", "errand-1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.post(MUNICIPALITY_ID, NAMESPACE, "errand-1", new CreateMessage("OUTBOUND", "body", "author", "missing-parent"), null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(repositoryMock, never()).save(any());
		verifyNoInteractions(eventsMock);
	}

	@Test
	void listForErrandReturnsMappedMessagesWithAttachments() {
		when(repositoryMock.findByErrandIdOrderByCreatedAsc("errand-1")).thenReturn(List.of(
			MessageEntity.create().withId("m1").withErrandId("errand-1").withDirection("INBOUND").withBody("b1").withAuthor("a1").withCreated(FIXED_TIMESTAMP)));
		when(attachmentRepositoryMock.findByMessageIdIn(List.of("m1"))).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("att-1").withMessageId("m1").withFileName("f.pdf")));

		final var result = service.listForErrand(MUNICIPALITY_ID, NAMESPACE, "errand-1");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("m1");
		assertThat(result.getFirst().getDirection()).isEqualTo("INBOUND");
		assertThat(result.getFirst().getBody()).isEqualTo("b1");
		assertThat(result.getFirst().getAttachments()).hasSize(1);
		assertThat(result.getFirst().getAttachments().getFirst().getId()).isEqualTo("att-1");
	}

	@Test
	void listForUnknownErrandIsNotFound() {
		errandMissing("errand-1");

		assertThatThrownBy(() -> service.listForErrand(MUNICIPALITY_ID, NAMESPACE, "errand-1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(repositoryMock);
	}

	@Test
	void readReturnsMessageScopedToErrand() {
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.of(
			MessageEntity.create().withId("m1").withErrandId("e1").withDirection("OUTBOUND").withBody("b")));
		when(attachmentRepositoryMock.findByMessageId("m1")).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("att-1").withMessageId("m1")));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, "e1", "m1");

		assertThat(result.getId()).isEqualTo("m1");
		assertThat(result.getBody()).isEqualTo("b");
		assertThat(result.getAttachments()).hasSize(1);
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void readNotFoundOnOtherErrand() {
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, "e1", "m1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readNotFoundOnUnknownErrand() {
		errandMissing("e1");

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, "e1", "m1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(repositoryMock);
	}

	@Test
	void streamAttachmentFileStreamsContent() {
		final var response = new MockHttpServletResponse();
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.of(MessageEntity.create().withId("m1")));
		when(attachmentRepositoryMock.findByMessageIdAndId("m1", "att-1")).thenReturn(Optional.of(
			MessageAttachmentEntity.create().withId("att-1").withFileName("f.pdf").withMimeType("application/pdf").withFileSize(8)));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("att-1")).thenReturn(Optional.of(
			MessageAttachmentDataEntity.create().withFile(new MariaDbBlob("contents".getBytes()))));

		service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, "e1", "m1", "att-1", response);

		assertThat(new String(response.getContentAsByteArray(), UTF_8)).isEqualTo("contents");
		assertThat(response.getContentType()).isEqualTo("application/pdf");
		assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"f.pdf\"");
	}

	@Test
	void streamAttachmentFileMessageNotFound() {
		final var response = new MockHttpServletResponse();
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, "e1", "m1", "att-1", response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(attachmentRepositoryMock, attachmentDataRepositoryMock);
	}

	@Test
	void streamAttachmentFileAttachmentNotFound() {
		final var response = new MockHttpServletResponse();
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.of(MessageEntity.create().withId("m1")));
		when(attachmentRepositoryMock.findByMessageIdAndId("m1", "att-1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, "e1", "m1", "att-1", response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(attachmentDataRepositoryMock);
	}

	@Test
	void streamAttachmentFileDataNotFound() {
		final var response = new MockHttpServletResponse();
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.of(MessageEntity.create().withId("m1")));
		when(attachmentRepositoryMock.findByMessageIdAndId("m1", "att-1")).thenReturn(Optional.of(
			MessageAttachmentEntity.create().withId("att-1")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("att-1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, "e1", "m1", "att-1", response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void streamAttachmentFileSqlExceptionWrappedAsInternalServerError() throws SQLException {
		final var response = new MockHttpServletResponse();
		final var blob = mock(Blob.class);
		when(blob.getBinaryStream()).thenThrow(new SQLException("boom"));
		when(repositoryMock.findByIdAndErrandId("m1", "e1")).thenReturn(Optional.of(MessageEntity.create().withId("m1")));
		when(attachmentRepositoryMock.findByMessageIdAndId("m1", "att-1")).thenReturn(Optional.of(
			MessageAttachmentEntity.create().withId("att-1").withFileName("f.pdf").withMimeType("application/pdf")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("att-1")).thenReturn(Optional.of(
			MessageAttachmentDataEntity.create().withFile(blob)));

		assertThatThrownBy(() -> service.streamAttachmentFile(MUNICIPALITY_ID, NAMESPACE, "e1", "m1", "att-1", response))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);
	}
}
