package se.sundsvall.caremanagement.notifications.service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.conversation.service.event.MessagePosted;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;

import static org.springframework.util.StringUtils.hasText;

/**
 * Raises a notification for the assigned caseworker (handläggare) whenever the applicant posts an INBOUND message on an
 * errand. The notification is content-free — it only signals that a new message exists; the body stays in the in-app
 * conversation thread. The recipient ({@code ownerId}) is the errand's assigned user; an unassigned errand has no
 * recipient, so no notification is raised. Runs asynchronously in its own transaction after the message-post commits
 * (the {@link MessagePosted} event is durably staged in Spring Modulith's outbox in between).
 */
@Component
class MessageNotificationListener {

	private static final Logger LOG = LoggerFactory.getLogger(MessageNotificationListener.class);

	private static final String INBOUND = "INBOUND";
	private static final String DESCRIPTION = "New message from the applicant";

	private final ErrandRepository errandRepository;
	private final NotificationService notificationService;

	MessageNotificationListener(final ErrandRepository errandRepository, final NotificationService notificationService) {
		this.errandRepository = errandRepository;
		this.notificationService = notificationService;
	}

	@ApplicationModuleListener
	void on(final MessagePosted event) {
		if (!INBOUND.equals(event.direction())) {
			return;
		}

		errandRepository.findByIdAndNamespaceAndMunicipalityId(event.errandId(), event.namespace(), event.municipalityId())
			.map(ErrandEntity::getAssignedUserId)
			.filter(ownerId -> hasText(ownerId))
			.ifPresent(ownerId -> {
				try {
					notificationService.create(event.municipalityId(), event.namespace(), event.errandId(), Notification.create()
						.withOwnerId(ownerId)
						.withCreatedBy(event.author())
						.withType("CREATE")
						.withSubType("MESSAGE")
						.withDescription(DESCRIPTION));
				} catch (final RuntimeException e) {
					LOG.warn("Failed to create message notification for errand {}", event.errandId(), e);
				}
			});
	}
}
