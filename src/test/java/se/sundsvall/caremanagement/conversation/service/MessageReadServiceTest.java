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
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.CUSTOM;
import static se.sundsvall.dept44.support.Identifier.Type.PARTY_ID;

@ExtendWith(MockitoExtension.class)
class MessageReadServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	// The caller identity resolves to a conversation side inside the service: adAccount → caseworker (reads INBOUND),
	// partyId → applicant/client (reads OUTBOUND). markRead's readBy comes from the identity value.
	private static final Identifier CASEWORKER = Identifier.create().withType(AD_ACCOUNT).withValue("joe001doe");
	private static final Identifier CLIENT = Identifier.create().withType(PARTY_ID).withValue("f47ac10b-58cc-4372-a567-0e02b2c3d479");

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@Mock
	private MessageRepository messageRepositoryMock;

	@Mock
	private MessageReadReceiptRepository receiptRepositoryMock;

	@InjectMocks
	private MessageReadService service;

	private void errandMissing() {
		doThrow(Problem.valueOf(NOT_FOUND, "No errand"))
			.when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	private static MessageEntity message(final String id, final String direction) {
		return MessageEntity.create().withId(id).withErrandId(ERRAND_ID).withDirection(direction);
	}

	@Test
	void unreadCountForCaseworkerCountsInbound() {
		when(receiptRepositoryMock.countUnread(ERRAND_ID, "INBOUND", "CASEWORKER")).thenReturn(3L);

		assertThat(service.unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER)).isEqualTo(3L);
		verify(receiptRepositoryMock).countUnread(ERRAND_ID, "INBOUND", "CASEWORKER");
	}

	@Test
	void unreadCountForClientCountsOutbound() {
		when(receiptRepositoryMock.countUnread(ERRAND_ID, "OUTBOUND", "CLIENT")).thenReturn(2L);

		assertThat(service.unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CLIENT)).isEqualTo(2L);
		verify(receiptRepositoryMock).countUnread(ERRAND_ID, "OUTBOUND", "CLIENT");
	}

	@Test
	void unreadCountOnUnknownErrandIsNotFound() {
		errandMissing();

		assertThatThrownBy(() -> service.unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No errand");

		verifyNoInteractions(receiptRepositoryMock);
	}

	@Test
	void unreadCountWithUnresolvableIdentityIsBadRequest() {
		final var custom = Identifier.create().withType(CUSTOM).withValue("whoever");

		assertThatThrownBy(() -> service.unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, custom))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: Cannot determine conversation side from header 'X-Sent-By' — expected type=adAccount (caseworker) or type=partyId (applicant)");

		verifyNoInteractions(receiptRepositoryMock);
	}

	@Test
	void markReadCreatesReceiptsForUnreadAddressedMessages() {
		final var ids = List.of("m1", "m2");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("m1", "INBOUND"), message("m2", "INBOUND")));
		when(receiptRepositoryMock.findReadMessageIds("CASEWORKER", List.of("m1", "m2"))).thenReturn(emptyList());

		service.markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER, ids);

		final ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);
		verify(receiptRepositoryMock, times(2)).insertIgnore(anyString(), messageIdCaptor.capture(), eq("CASEWORKER"), eq("joe001doe"), any());
		assertThat(messageIdCaptor.getAllValues()).containsExactly("m1", "m2");
	}

	@Test
	void markReadOnUnknownErrandIsNotFound() {
		errandMissing();

		assertThatThrownBy(() -> service.markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER, List.of("m1")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No errand");

		verifyNoInteractions(messageRepositoryMock, receiptRepositoryMock);
	}

	@Test
	void markReadSkipsAlreadyReadMessages() {
		final var ids = List.of("m1", "m2");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("m1", "INBOUND"), message("m2", "INBOUND")));
		when(receiptRepositoryMock.findReadMessageIds("CASEWORKER", List.of("m1", "m2"))).thenReturn(List.of("m1"));

		service.markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER, ids);

		verify(receiptRepositoryMock).insertIgnore(anyString(), eq("m2"), eq("CASEWORKER"), eq("joe001doe"), any());
		verify(receiptRepositoryMock, never()).insertIgnore(anyString(), eq("m1"), anyString(), anyString(), any());
	}

	@Test
	void markReadIgnoresMessagesAddressedToTheOtherSide() {
		final var ids = List.of("own-1");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("own-1", "OUTBOUND"))); // caseworker's own message, not addressed to caseworker

		service.markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER, ids);

		verify(receiptRepositoryMock, never()).findReadMessageIds(anyString(), anyList());
		verify(receiptRepositoryMock, never()).insertIgnore(anyString(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void markReadDeduplicatesIds() {
		final var ids = List.of("m1", "m1");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, List.of("m1")))
			.thenReturn(List.of(message("m1", "INBOUND")));
		when(receiptRepositoryMock.findReadMessageIds("CASEWORKER", List.of("m1"))).thenReturn(emptyList());

		service.markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER, ids);

		verify(receiptRepositoryMock, times(1)).insertIgnore(anyString(), eq("m1"), eq("CASEWORKER"), eq("joe001doe"), any());
	}

	@Test
	void markReadThrowsWhenAMessageIsNotOnTheErrand() {
		final var ids = List.of("m1", "missing");
		when(messageRepositoryMock.findByErrandIdAndIdIn(ERRAND_ID, ids))
			.thenReturn(List.of(message("m1", "INBOUND")));

		assertThatThrownBy(() -> service.markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, CASEWORKER, ids))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Message ids [missing] were not found on errand 'errand-1'");

		verify(receiptRepositoryMock, never()).insertIgnore(any(), any(), any(), any(), any());
	}
}
