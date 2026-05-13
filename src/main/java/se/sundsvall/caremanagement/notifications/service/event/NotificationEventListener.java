package se.sundsvall.caremanagement.notifications.service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;
import se.sundsvall.caremanagement.shared.NotificationRequest;

@Component
class NotificationEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationEventListener.class);

	private final NotificationService notificationService;

	NotificationEventListener(final NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@TransactionalEventListener
	void onNotificationRequested(final NotificationRequest event) {
		try {
			final var notification = Notification.create()
				.withOwnerId(event.ownerId())
				.withCreatedBy(event.createdBy())
				.withType(event.type())
				.withSubType(event.subType())
				.withDescription(event.description());
			notificationService.create(event.municipalityId(), event.namespace(), event.errandId(), notification);
		} catch (final RuntimeException e) {
			LOG.warn("Failed to create notification for errand {}", event.errandId(), e);
		}
	}
}
