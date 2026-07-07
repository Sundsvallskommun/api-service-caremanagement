package se.sundsvall.caremanagement.notifications.service.event;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.conversation.service.event.MessageCreated;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;

/**
 * Raises a notification whenever the applicant posts an INBOUND message on an errand. The notification is content-free
 * —
 * it only signals that a new message exists; the body stays in the in-app conversation thread. The recipient
 * ({@code ownerId}) is the errand's assigned caseworker when there is one; if the errand is still
 * unassigned the notification is raised ownerless ({@code ownerId == null}), so the errand still shows up in the
 * owner-agnostic unread-notification filter and is claimed by whoever picks it up (see
 * {@link ErrandAssignmentNotificationListener}). A missing errand raises nothing. Runs asynchronously in its own
 * transaction after the message-post commits (the {@link MessageCreated} event is durably staged in Spring Modulith's
 * outbox in between).
 */
@Component
class MessageNotificationListener {

	private static final Logger LOG = LoggerFactory.getLogger(MessageNotificationListener.class);

	private static final String INBOUND = "INBOUND";
	private static final String DESCRIPTION = "Nytt meddelande från sökanden";

	private final ErrandQueryService errandQueryService;
	private final NotificationService notificationService;

	MessageNotificationListener(final ErrandQueryService errandQueryService, final NotificationService notificationService) {
		this.errandQueryService = errandQueryService;
		this.notificationService = notificationService;
	}

	@ApplicationModuleListener
	void on(final MessageCreated event) {
		if (!INBOUND.equals(event.direction())) {
			return;
		}

		errandQueryService.findErrand(event.municipalityId(), event.namespace(), event.errandId())
			.ifPresent(errand -> {
				final var ownerId = Optional.ofNullable(errand.getAssignedUserId())
					.filter(StringUtils::hasText)
					.orElse(null);
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
