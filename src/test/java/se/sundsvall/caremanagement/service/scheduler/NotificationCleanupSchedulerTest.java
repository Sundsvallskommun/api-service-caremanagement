package se.sundsvall.caremanagement.service.scheduler;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.integration.db.NotificationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@InjectMocks
	private NotificationCleanupScheduler scheduler;

	@Test
	void cleanupExpiredNotifications_callsRepoWithCurrentTimestamp() {
		when(notificationRepositoryMock.deleteByExpiresBefore(any(OffsetDateTime.class))).thenReturn(3L);

		final var before = OffsetDateTime.now().minusSeconds(1);
		scheduler.cleanupExpiredNotifications();
		final var after = OffsetDateTime.now().plusSeconds(1);

		final var captor = ArgumentCaptor.forClass(OffsetDateTime.class);
		verify(notificationRepositoryMock).deleteByExpiresBefore(captor.capture());
		assertThat(captor.getValue()).isAfter(before).isBefore(after);
	}
}
