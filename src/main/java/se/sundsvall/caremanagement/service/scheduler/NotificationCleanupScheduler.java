package se.sundsvall.caremanagement.service.scheduler;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.integration.db.NotificationRepository;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Component
class NotificationCleanupScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationCleanupScheduler.class);

	private final NotificationRepository notificationRepository;

	NotificationCleanupScheduler(final NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Dept44Scheduled(cron = "${scheduler.notification-cleanup.cron}",
		name = "${scheduler.notification-cleanup.name}",
		lockAtMostFor = "${scheduler.notification-cleanup.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.notification-cleanup.maximum-execution-time}")
	void cleanupExpiredNotifications() {
		final var cutoff = OffsetDateTime.now(ZoneId.systemDefault());
		final var deleted = notificationRepository.deleteByExpiresBefore(cutoff);
		LOG.info("Deleted {} expired notification(s) (cutoff {})", deleted, cutoff);
	}
}
