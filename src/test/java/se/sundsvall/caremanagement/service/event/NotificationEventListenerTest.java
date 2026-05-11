package se.sundsvall.caremanagement.service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.api.model.Notification;
import se.sundsvall.caremanagement.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";

	@Mock
	private NotificationService notificationServiceMock;

	@InjectMocks
	private NotificationEventListener listener;

	@Test
	void onNotificationRequested_delegatesToService() {
		final var notification = Notification.create().withOwnerId("o").withType("CREATE").withDescription("d");
		final var event = new NotificationRequestedEvent(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, notification);

		listener.onNotificationRequested(event);

		verify(notificationServiceMock).create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, notification);
	}

	@Test
	void onNotificationRequested_swallowsRuntimeException() {
		when(notificationServiceMock.create(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
		final var event = new NotificationRequestedEvent(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			Notification.create().withOwnerId("o").withType("CREATE").withDescription("d"));

		listener.onNotificationRequested(event);

		verify(notificationServiceMock).create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, event.notification());
	}
}
