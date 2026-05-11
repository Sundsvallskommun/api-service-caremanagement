package se.sundsvall.caremanagement.service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import se.sundsvall.caremanagement.service.NotificationService;

@Component
class NotificationEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(NotificationEventListener.class);

	private final NotificationService notificationService;

	NotificationEventListener(final NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@TransactionalEventListener
	void onNotificationRequested(final NotificationRequestedEvent event) {
		try {
			notificationService.create(event.municipalityId(), event.namespace(), event.errandId(), event.notification());
		} catch (final RuntimeException e) {
			LOG.warn("Failed to create notification for errand {}", event.errandId(), e);
		}
	}
}
