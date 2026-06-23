package se.sundsvall.caremanagement.conversation.spi;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationThreadQueryServiceTest {

	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private MessageAttachmentRepository attachmentRepositoryMock;

	@InjectMocks
	private ConversationThreadQueryService service;

	@Test
	void mapsThreadOldestFirstWithAttachmentFileNames() {
		final var first = MessageEntity.create().withId("m1").withErrandId(ERRAND_ID).withDirection("INBOUND")
			.withBody("Hej").withAuthor("joe01doe").withCreated(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
		final var second = MessageEntity.create().withId("m2").withErrandId(ERRAND_ID).withDirection("OUTBOUND")
			.withBody("Svar").withAuthor("agent").withCreated(OffsetDateTime.parse("2026-06-02T09:00:00+02:00"));

		final var attachment = MessageAttachmentEntity.create().withId("a1").withMessageId("m1").withFileName("intyg.pdf")
			.withMimeType("application/pdf").withSenderRole("CLIENT").withCreated(OffsetDateTime.now());

		when(messageRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(List.of(first, second));
		when(attachmentRepositoryMock.findByMessageIdIn(List.of("m1", "m2"))).thenReturn(List.of(attachment));

		final var thread = service.threadForErrand(ERRAND_ID);

		assertThat(thread).hasSize(2);
		assertThat(thread.get(0).direction()).isEqualTo("INBOUND");
		assertThat(thread.get(0).body()).isEqualTo("Hej");
		assertThat(thread.get(0).author()).isEqualTo("joe01doe");
		assertThat(thread.get(0).attachmentFileNames()).containsExactly("intyg.pdf");
		assertThat(thread.get(1).attachmentFileNames()).isEmpty();
	}

	@Test
	void emptyThread() {
		when(messageRepositoryMock.findByErrandIdOrderByCreatedAsc(ERRAND_ID)).thenReturn(emptyList());
		when(attachmentRepositoryMock.findByMessageIdIn(emptyList())).thenReturn(emptyList());

		assertThat(service.threadForErrand(ERRAND_ID)).isEmpty();
	}
}
