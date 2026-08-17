package se.sundsvall.caremanagement.notifications.service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandAssigned;
import se.sundsvall.caremanagement.notifications.service.NotificationService;

/**
 * Backfills the recipient of ownerless notifications when an errand is assigned. While an errand is unassigned, an
 * applicant's INBOUND message raises an ownerless MESSAGE notification (see {@link MessageNotificationListener}); once
 * a
 * caseworker picks the errand up, those notifications are claimed for the new assignee so they appear in the
 * caseworker's unread list. Reassignment of an already-owned errand is a no-op (nothing left ownerless). Runs
 * asynchronously in its own transaction after the assignment commits ({@link ErrandAssigned} is durably staged in
 * Spring Modulith's outbox in between).
 */
@Component
class ErrandAssignmentNotificationListener {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandAssignmentNotificationListener.class);

	private final NotificationService notificationService;

	ErrandAssignmentNotificationListener(final NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@ApplicationModuleListener
	void assignOwnerlessNotifications(final ErrandAssigned event) {
		try {
			notificationService.assignUnownedNotifications(event.municipalityId(), event.namespace(), event.errandId(), event.newAssignee());
		} catch (final RuntimeException e) {
			LOG.warn("Failed to assign ownerless notifications for errand {}", event.errandId(), e);
		}
	}
}
