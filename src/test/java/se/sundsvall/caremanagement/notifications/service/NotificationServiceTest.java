package se.sundsvall.caremanagement.notifications.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.notifications.integration.db.NotificationRepository;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@Mock
	private NotificationProperties propertiesMock;

	@InjectMocks
	private NotificationService service;

	@Test
	void assignUnownedNotificationsDelegatesToRepository() {
		when(notificationRepositoryMock.assignOwnerToUnowned(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "jane01doe")).thenReturn(2);

		final var updated = service.assignUnownedNotifications(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "jane01doe");

		assertThat(updated).isEqualTo(2);
		verify(notificationRepositoryMock).assignOwnerToUnowned(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "jane01doe");
	}

	@Test
	void assignUnownedNotificationsIsNoOpForBlankOwner() {
		final var updated = service.assignUnownedNotifications(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, " ");

		assertThat(updated).isZero();
		verifyNoInteractions(notificationRepositoryMock);
	}
}
