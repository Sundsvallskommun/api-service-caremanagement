package se.sundsvall.caremanagement.notifications.service.scheduler;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.notifications.integration.db.NotificationRepository;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@InjectMocks
	private NotificationCleanupScheduler scheduler;

	@Test
	void cleanupExpiredNotificationsCallsRepoWithCurrentTimestamp() {
		when(notificationRepositoryMock.deleteByExpiresBefore(any(OffsetDateTime.class))).thenReturn(3L);

		scheduler.cleanupExpiredNotifications();

		final var captor = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(notificationRepositoryMock).deleteByExpiresBefore(captor.capture());
		assertThat(captor.getValue()).isCloseTo(OffsetDateTime.now(), within(1, SECONDS));
		verifyNoMoreInteractions(notificationRepositoryMock);
	}
}
