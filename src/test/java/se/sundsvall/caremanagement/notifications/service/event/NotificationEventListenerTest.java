package se.sundsvall.caremanagement.notifications.service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;
import se.sundsvall.caremanagement.shared.NotificationRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
		final var event = new NotificationRequest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "owner", "creator", "CREATE", "ERRAND", "description");

		listener.onNotificationRequested(event);

		verify(notificationServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Notification.class));
	}

	@Test
	void onNotificationRequested_swallowsRuntimeException() {
		when(notificationServiceMock.create(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
		final var event = new NotificationRequest(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "owner", "creator", "CREATE", "ERRAND", "description");

		listener.onNotificationRequested(event);

		verify(notificationServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Notification.class));
	}
}
