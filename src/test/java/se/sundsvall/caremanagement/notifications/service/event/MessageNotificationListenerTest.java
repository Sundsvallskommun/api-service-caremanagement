package se.sundsvall.caremanagement.notifications.service.event;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.conversation.service.event.MessageCreated;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageNotificationListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String MESSAGE_ID = "msg-1";
	private static final String APPLICANT = "199001011234";

	@Mock
	private ErrandQueryService errandQueryServiceMock;

	@Mock
	private NotificationService notificationServiceMock;

	@InjectMocks
	private MessageNotificationListener listener;

	@Test
	void inboundMessageNotifiesAssignedCaseworker() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withAssignedUserId("jane01doe")));

		listener.notifyOnInboundMessage(inbound());

		final var captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		final var notification = captor.getValue();
		assertThat(notification.getOwnerId()).isEqualTo("jane01doe");
		assertThat(notification.getCreatedBy()).isEqualTo(APPLICANT);
		assertThat(notification.getType()).isEqualTo("CREATE");
		assertThat(notification.getSubType()).isEqualTo("MESSAGE");
		assertThat(notification.getContent()).isNull();
	}

	@Test
	void outboundMessageIsIgnored() {
		listener.notifyOnInboundMessage(new MessageCreated(MESSAGE_ID, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "OUTBOUND", "jane01doe", false, OffsetDateTime.now()));

		verifyNoInteractions(errandQueryServiceMock, notificationServiceMock);
	}

	@Test
	void unassignedErrandRaisesOwnerlessNotification() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID)));

		listener.notifyOnInboundMessage(inbound());

		final var captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		final var notification = captor.getValue();
		assertThat(notification.getOwnerId()).isNull();
		assertThat(notification.getCreatedBy()).isEqualTo(APPLICANT);
		assertThat(notification.getSubType()).isEqualTo("MESSAGE");
	}

	@Test
	void blankAssigneeRaisesOwnerlessNotification() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withAssignedUserId(" ")));

		listener.notifyOnInboundMessage(inbound());

		final var captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		assertThat(captor.getValue().getOwnerId()).isNull();
	}

	@Test
	void missingErrandRaisesNoNotification() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.empty());

		listener.notifyOnInboundMessage(inbound());

		verify(notificationServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void serviceFailureIsSwallowed() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withAssignedUserId("jane01doe")));
		when(notificationServiceMock.create(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));

		listener.notifyOnInboundMessage(inbound());

		verify(notificationServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Notification.class));
	}

	private static MessageCreated inbound() {
		return new MessageCreated(MESSAGE_ID, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "INBOUND", APPLICANT, false, OffsetDateTime.now());
	}
}
