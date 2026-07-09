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
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentDataEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class ConversationThreadQueryServiceTest {

	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private MessageAttachmentRepository attachmentRepositoryMock;

	@Mock
	private MessageAttachmentDataRepository attachmentDataRepositoryMock;

	@InjectMocks
	private ConversationThreadQueryService service;

	@Test
	void mapsThreadOldestFirstWithAttachmentContent() {
		final var first = MessageEntity.create().withId("m1").withErrandId(ERRAND_ID).withDirection("INBOUND")
			.withBody("Hej").withAuthor("joe01doe").withCreated(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
		final var second = MessageEntity.create().withId("m2").withErrandId(ERRAND_ID).withDirection("OUTBOUND")
			.withBody("Svar").withAuthor("agent").withCreated(OffsetDateTime.parse("2026-06-02T09:00:00+02:00"));

		final var attachment = MessageAttachmentEntity.create().withId("a1").withMessageId("m1").withFileName("intyg.pdf").withMimeType("application/pdf");

		when(messageRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(first, second));
		when(attachmentRepositoryMock.findByMessageIdIn(List.of("m1", "m2"))).thenReturn(List.of(attachment));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("a1")).thenReturn(Optional.of(
			MessageAttachmentDataEntity.create().withFile(new MariaDbBlob("hello".getBytes()))));

		final var thread = service.threadForErrand(ERRAND_ID);

		assertThat(thread).hasSize(2);
		assertThat(thread.getFirst().direction()).isEqualTo("INBOUND");
		assertThat(thread.getFirst().body()).isEqualTo("Hej");
		assertThat(thread.getFirst().author()).isEqualTo("joe01doe");
		assertThat(thread.getFirst().attachments()).hasSize(1);
		assertThat(thread.getFirst().attachments().getFirst().fileName()).isEqualTo("intyg.pdf");
		assertThat(thread.getFirst().attachments().getFirst().mimeType()).isEqualTo("application/pdf");
		assertThat(new String(thread.getFirst().attachments().getFirst().content(), UTF_8)).isEqualTo("hello");
		assertThat(thread.getLast().attachments()).isEmpty();
	}

	@Test
	void omitsAttachmentsWithoutData() {
		final var message = MessageEntity.create().withId("m1").withErrandId(ERRAND_ID).withDirection("INBOUND").withBody("Hej");
		when(messageRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(message));
		when(attachmentRepositoryMock.findByMessageIdIn(List.of("m1"))).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("a1").withMessageId("m1").withFileName("intyg.pdf")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("a1")).thenReturn(Optional.empty());

		final var thread = service.threadForErrand(ERRAND_ID);

		assertThat(thread).hasSize(1);
		assertThat(thread.getFirst().attachments()).isEmpty();
	}

	@Test
	void wrapsSqlExceptionAsProblem() throws SQLException {
		final var blob = mock(Blob.class);
		when(blob.getBinaryStream()).thenThrow(new SQLException("boom"));
		final var message = MessageEntity.create().withId("m1").withErrandId(ERRAND_ID).withDirection("INBOUND").withBody("Hej");
		when(messageRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(message));
		when(attachmentRepositoryMock.findByMessageIdIn(List.of("m1"))).thenReturn(List.of(
			MessageAttachmentEntity.create().withId("a1").withMessageId("m1").withFileName("intyg.pdf")));
		when(attachmentDataRepositoryMock.findByMessageAttachmentId("a1")).thenReturn(Optional.of(
			MessageAttachmentDataEntity.create().withFile(blob)));

		assertThatThrownBy(() -> service.threadForErrand(ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR)
			.hasMessage("Internal Server Error: Could not read conversation attachment content for attachment id 'a1': boom");
	}

	@Test
	void emptyThread() {
		when(messageRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(emptyList());
		when(attachmentRepositoryMock.findByMessageIdIn(emptyList())).thenReturn(emptyList());

		assertThat(service.threadForErrand(ERRAND_ID)).isEmpty();
	}
}
