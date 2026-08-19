package se.sundsvall.caremanagement.notifications.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandAssigned;
import se.sundsvall.caremanagement.notifications.service.NotificationService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandAssignmentNotificationListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private NotificationService notificationServiceMock;

	@InjectMocks
	private ErrandAssignmentNotificationListener listener;

	@Test
	void claimsOwnerlessNotificationsForNewAssignee() {
		listener.assignOwnerlessNotifications(assigned("jane01doe"));

		verify(notificationServiceMock).assignUnownedNotifications(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "jane01doe");
	}

	@Test
	void serviceFailureIsSwallowed() {
		when(notificationServiceMock.assignUnownedNotifications(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "jane01doe"))
			.thenThrow(new RuntimeException("boom"));

		listener.assignOwnerlessNotifications(assigned("jane01doe"));

		verify(notificationServiceMock).assignUnownedNotifications(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "jane01doe");
	}

	private static ErrandAssigned assigned(final String newAssignee) {
		return new ErrandAssigned(ERRAND_ID, "financial-assistance", MUNICIPALITY_ID, NAMESPACE, null, newAssignee, null, OffsetDateTime.now());
	}
}
