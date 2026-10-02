package se.sundsvall.caremanagement.notifications.service;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.integration.db.NotificationRepository;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

	@Test
	void handleAllDelegatesToRepository() {
		when(notificationRepositoryMock.handleAllByErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(3);

		final var updated = service.handleAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(updated).isEqualTo(3);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(notificationRepositoryMock).handleAllByErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void createMarksSelfCreatedNotificationAcknowledgedAndHandled() {
		when(propertiesMock.ttl()).thenReturn(Duration.ofDays(30));
		when(notificationRepositoryMock.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Notification.create()
			.withOwnerId("jane01doe")
			.withCreatedBy("jane01doe")
			.withType("CREATE")
			.withDescription("Something I did myself"));

		final var captor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().isAcknowledged()).isTrue();
		assertThat(captor.getValue().isHandled()).isTrue();
	}

	@Test
	void createLeavesForeignNotificationUnacknowledgedAndUnhandled() {
		when(propertiesMock.ttl()).thenReturn(Duration.ofDays(30));
		when(notificationRepositoryMock.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Notification.create()
			.withOwnerId("jane01doe")
			.withCreatedBy("john02doe")
			.withType("CREATE")
			.withDescription("Something somebody else did"));

		final var captor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().isAcknowledged()).isFalse();
		assertThat(captor.getValue().isHandled()).isFalse();
	}
}
