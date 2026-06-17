package se.sundsvall.caremanagement.attachments.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.conversation.service.event.MessagePosted;

/**
 * Keeps the consolidated client-attachment PDF current. Whenever the applicant posts an INBOUND message that carries
 * attachments, the consolidated PDF of all the errand's client-sent conversation attachments is rebuilt in place.
 * {@code @ApplicationModuleListener} runs asynchronously in a fresh transaction after the message-post commits, with
 * the event durably staged in Spring Modulith's outbox in between — so the rebuild is eventually consistent and the
 * conversation module stays decoupled from attachments.
 */
@Component
class AttachmentConversationListener {

	private static final String INBOUND = "INBOUND";

	private final AttachmentService attachmentService;

	AttachmentConversationListener(final AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@ApplicationModuleListener
	void on(final MessagePosted event) {
		if (INBOUND.equals(event.direction()) && event.hasAttachments()) {
			attachmentService.regenerateClientAttachmentPdf(event.municipalityId(), event.namespace(), event.errandId());
		}
	}
}
