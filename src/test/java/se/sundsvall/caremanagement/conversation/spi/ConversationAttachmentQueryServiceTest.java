package se.sundsvall.caremanagement.conversation.spi;

import java.sql.Blob;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mariadb.jdbc.MariaDbBlob;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentDataRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentDataEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class ConversationAttachmentQueryServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final String ERRAND_ID = "e1";

	@Mock
	private MessageAttachmentRepository attachmentRepositoryMock;

	@Mock
	private MessageAttachmentDataRepository attachmentDataRepositoryMock;

	@InjectMocks
	private ConversationAttachmentQueryService service;

	@Test
	void listForErrandMapsAllAttachments() {
		when(attachmentRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("a1").withMessageId("m1").withFileName("intyg.pdf").withMimeType("application/pdf").withFileSize(4).withSenderRole("CLIENT").withCreated(FIXED_TIMESTAMP),
			MessageAttachmentEntity.create().withId("a2").withMessageId("m2").withFileName("beslut.pdf").withMimeType("application/pdf").withFileSize(8).withSenderRole("CASEWORKER").withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand(ERRAND_ID);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().id()).isEqualTo("a1");
		assertThat(result.getFirst().messageId()).isEqualTo("m1");
		assertThat(result.getFirst().fileName()).isEqualTo("intyg.pdf");
		assertThat(result.getFirst().mimeType()).isEqualTo("application/pdf");
		assertThat(result.getFirst().fileSize()).isEqualTo(4);
		assertThat(result.getFirst().senderRole()).isEqualTo("CLIENT");
		assertThat(result.getFirst().created()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(result.getLast().senderRole()).isEqualTo("CASEWORKER");
	}

	@Test
	void clientAttachmentContentsForErrandReturnsClientBytesOnly() {
		when(attachmentRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("a1").withFileName("intyg.pdf").withMimeType("application/pdf").withSenderRole("CLIENT"),
			MessageAttachmentEntity.create().withId("a2").withFileName("beslut.pdf").withMimeType("application/pdf").withSenderRole("CASEWORKER")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("a1")).thenReturn(Optional.of(
			MessageAttachmentDataEntity.create().withFile(new MariaDbBlob("hello".getBytes()))));

		final var result = service.clientAttachmentContentsForErrand(ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().fileName()).isEqualTo("intyg.pdf");
		assertThat(result.getFirst().mimeType()).isEqualTo("application/pdf");
		assertThat(new String(result.getFirst().content(), UTF_8)).isEqualTo("hello");
		// The caseworker attachment's blob is never loaded.
		verify(attachmentDataRepositoryMock, never()).findByMessageAttachmentId("a2");
	}

	@Test
	void clientAttachmentContentsForErrandSkipsAttachmentsWithoutData() {
		when(attachmentRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("a1").withFileName("intyg.pdf").withSenderRole("CLIENT")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("a1")).thenReturn(Optional.empty());

		assertThat(service.clientAttachmentContentsForErrand(ERRAND_ID)).isEmpty();
	}

	@Test
	void clientAttachmentContentsForErrandWrapsSqlExceptionAsProblem() throws SQLException {
		final var blob = mock(Blob.class);
		when(blob.getBinaryStream()).thenThrow(new SQLException("boom"));
		when(attachmentRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("a1").withFileName("intyg.pdf").withSenderRole("CLIENT")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("a1")).thenReturn(Optional.of(
			MessageAttachmentDataEntity.create().withFile(blob)));

		assertThatThrownBy(() -> service.clientAttachmentContentsForErrand(ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);
	}
}
