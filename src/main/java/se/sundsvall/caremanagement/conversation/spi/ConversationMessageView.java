package se.sundsvall.caremanagement.conversation.spi;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only cross-module view of a single conversation message, for archiving the thread into a document. Carries the
 * message body and metadata plus its attachments (file name, MIME type and content). Exposed via the {@code spi} named
 * interface so other modules can render and merge the thread without reaching into the conversation persistence layer.
 *
 * @param direction   {@code INBOUND} (applicant → caseworker) or {@code OUTBOUND} (caseworker → applicant)
 * @param body        the message text
 * @param author      the sender id (may be {@code null})
 * @param created     when the message was posted
 * @param attachments the message's attachments, in upload order (never {@code null})
 */
public record ConversationMessageView(
	String direction,
	String body,
	String author,
	OffsetDateTime created,
	List<ConversationAttachmentView> attachments) {
}
