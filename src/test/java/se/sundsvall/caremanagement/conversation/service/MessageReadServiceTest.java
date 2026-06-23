package se.sundsvall.caremanagement.conversation.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.conversation.integration.db.MessageReadReceiptRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageReadReceiptEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.conversation.service.ReaderSide.CASEWORKER;
import static se.sundsvall.caremanagement.conversation.service.ReaderSide.CLIENT;

@ExtendWith(MockitoExtension.class)
class MessageReadServiceTest {

	private static final String ERRAND_ID = "errand-1";

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private MessageReadReceiptRepository receiptRepositoryMock;

	@InjectMocks
	private MessageReadService service;

	private static MessageEntity message(final String id, final String direction) {
		return MessageEntity.create().withId(id).withErrandId(ERRAND_ID).withDirection(direction);
	}

	@Test
	void unreadCountForCaseworkerCountsInbound() {
		when(receiptRepositoryMock.countUnread(ERRAND_ID, "INBOUND", "CASEWORKER")).thenReturn(3L);

		assertThat(service.unreadCount(ERRAND_ID, CASEWORKER)).isEqualTo(3L);
		verify(receiptRepositoryMock).countUnread(ERRAND_ID, "INBOUND", "CASEWORKER");
	}

	@Test
	void unreadCountForClientCountsOutbound() {
		when(receiptRepositoryMock.countUnread(ERRAND_ID, "OUTBOUND", "CLIENT")).thenReturn(2L);

		assertThat(service.unreadCount(ERRAND_ID, CLIENT)).isEqualTo(2L);
		verify(receiptRepositoryMock).countUnread(ERRAND_ID, "OUTBOUND", "CLIENT");
	}

	@Test
	void markReadCreatesReceiptsForUnreadAddressedMessages() {
		final var ids = List.of("m1", "m2");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("m1", "INBOUND"), message("m2", "INBOUND")));
		when(receiptRepositoryMock.findReadMessageIds("CASEWORKER", List.of("m1", "m2"))).thenReturn(emptyList());

		service.markRead(ERRAND_ID, CASEWORKER, "joe001doe", ids);

		final ArgumentCaptor<List<MessageReadReceiptEntity>> captor = ArgumentCaptor.captor();
		verify(receiptRepositoryMock).saveAll(captor.capture());
		assertThat(captor.getValue())
			.extracting(MessageReadReceiptEntity::getMessageId, MessageReadReceiptEntity::getReaderSide, MessageReadReceiptEntity::getReadBy)
			.containsExactly(
				tuple("m1", "CASEWORKER", "joe001doe"),
				tuple("m2", "CASEWORKER", "joe001doe"));
		assertThat(captor.getValue()).allSatisfy(receipt -> assertThat(receipt.getReadAt()).isNotNull());
	}

	@Test
	void markReadSkipsAlreadyReadMessages() {
		final var ids = List.of("m1", "m2");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("m1", "INBOUND"), message("m2", "INBOUND")));
		when(receiptRepositoryMock.findReadMessageIds("CASEWORKER", List.of("m1", "m2"))).thenReturn(List.of("m1"));

		service.markRead(ERRAND_ID, CASEWORKER, "joe001doe", ids);

		final ArgumentCaptor<List<MessageReadReceiptEntity>> captor = ArgumentCaptor.captor();
		verify(receiptRepositoryMock).saveAll(captor.capture());
		assertThat(captor.getValue()).extracting(MessageReadReceiptEntity::getMessageId).containsExactly("m2");
	}

	@Test
	void markReadIgnoresMessagesAddressedToTheOtherSide() {
		final var ids = List.of("own-1");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("own-1", "OUTBOUND"))); // caseworker's own message, not addressed to caseworker

		service.markRead(ERRAND_ID, CASEWORKER, "joe001doe", ids);

		verify(receiptRepositoryMock, never()).findReadMessageIds(anyString(), anyList());
		verify(receiptRepositoryMock, never()).saveAll(any());
	}

	@Test
	void markReadDeduplicatesIds() {
		final var ids = List.of("m1", "m1");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, List.of("m1")))
			.thenReturn(List.of(message("m1", "INBOUND")));
		when(receiptRepositoryMock.findReadMessageIds("CASEWORKER", List.of("m1"))).thenReturn(emptyList());

		service.markRead(ERRAND_ID, CASEWORKER, "joe001doe", ids);

		final ArgumentCaptor<List<MessageReadReceiptEntity>> captor = ArgumentCaptor.captor();
		verify(receiptRepositoryMock).saveAll(captor.capture());
		assertThat(captor.getValue()).extracting(MessageReadReceiptEntity::getMessageId).containsExactly("m1");
	}

	@Test
	void markReadThrowsWhenAMessageIsNotOnTheErrand() {
		final var ids = List.of("m1", "missing");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("m1", "INBOUND")));

		assertThatThrownBy(() -> service.markRead(ERRAND_ID, CASEWORKER, "joe001doe", ids))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining("missing");

		verify(receiptRepositoryMock, never()).saveAll(any());
	}
}
