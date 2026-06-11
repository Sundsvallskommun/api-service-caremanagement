package se.sundsvall.caremanagement.conversation.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.service.event.MessagePosted;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private MessageRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@InjectMocks
	private MessageService service;

	@Test
	void postPublishesEventAndReturnsId() {
		final var saved = MessageEntity.create().withId("message-1").withErrandId("errand-1").withDirection("OUTBOUND").withAuthor("author").withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.save(any(MessageEntity.class))).thenReturn(saved);

		final var id = service.post("errand-1", new CreateMessage("OUTBOUND", "body", "author"));

		assertThat(id).isEqualTo("message-1");

		final ArgumentCaptor<MessageEntity> entityCaptor = ArgumentCaptor.forClass(MessageEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo("errand-1");
		assertThat(entityCaptor.getValue().getDirection()).isEqualTo("OUTBOUND");
		assertThat(entityCaptor.getValue().getBody()).isEqualTo("body");
		assertThat(entityCaptor.getValue().getAuthor()).isEqualTo("author");
		assertThat(entityCaptor.getValue().getCreated()).isNotNull();

		final ArgumentCaptor<MessagePosted> eventCaptor = ArgumentCaptor.forClass(MessagePosted.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().messageId()).isEqualTo("message-1");
		assertThat(eventCaptor.getValue().errandId()).isEqualTo("errand-1");
		assertThat(eventCaptor.getValue().direction()).isEqualTo("OUTBOUND");
		assertThat(eventCaptor.getValue().author()).isEqualTo("author");
	}

	@Test
	void listForErrandReturnsMappedMessagesChronologically() {
		when(repositoryMock.findByErrandIdOrderByCreatedAsc("errand-1")).thenReturn(List.of(
			MessageEntity.create().withId("m1").withErrandId("errand-1").withDirection("INBOUND").withBody("b1").withAuthor("a1").withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand("errand-1");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("m1");
		assertThat(result.getFirst().getDirection()).isEqualTo("INBOUND");
		assertThat(result.getFirst().getBody()).isEqualTo("b1");
		assertThat(result.getFirst().getAuthor()).isEqualTo("a1");
		assertThat(result.getFirst().getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void readReturnsMessage() {
		when(repositoryMock.findById("m1")).thenReturn(Optional.of(
			MessageEntity.create().withId("m1").withErrandId("e1").withDirection("OUTBOUND").withBody("b")));

		final var result = service.read("m1");

		assertThat(result.getId()).isEqualTo("m1");
		assertThat(result.getBody()).isEqualTo("b");
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void readNotFound() {
		when(repositoryMock.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read("missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}
}
