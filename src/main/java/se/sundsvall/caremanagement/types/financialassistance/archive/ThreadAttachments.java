package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.conversation.spi.ConversationMessageView;

/**
 * Flattens a conversation thread into a single, globally-numbered list of attachments — the one place attachment
 * numbers
 * are assigned, so the per-message listing in the rendered messages, the separator pages and the appended order all use
 * the same {@code Bilaga {n}}. Numbering follows the thread: messages oldest-first, attachments in upload order within
 * each message. The number, the sender role and the message date together distinguish two attachments that happen to
 * share a file name.
 */
final class ThreadAttachments {

	static final String INBOUND = "INBOUND";
	static final String ROLE_APPLICANT = "Sökande";
	static final String ROLE_CASEWORKER = "Handläggare";

	private ThreadAttachments() {}

	/**
	 * A conversation attachment with its assigned number and the context of the message it was attached to.
	 *
	 * @param number       the 1-based number across the whole thread
	 * @param messageIndex the index of the owning message in the thread (oldest = 0)
	 * @param role         the sender role of the owning message ({@code Sökande} / {@code Handläggare})
	 * @param created      when the owning message was posted
	 * @param fileName     the attachment file name
	 * @param mimeType     the attachment MIME type
	 * @param content      the attachment bytes
	 */
	record NumberedAttachment(int number, int messageIndex, String role, OffsetDateTime created, String fileName, String mimeType, byte[] content) {}

	static String role(final String direction) {
		return INBOUND.equals(direction) ? ROLE_APPLICANT : ROLE_CASEWORKER;
	}

	static List<NumberedAttachment> flatten(final List<ConversationMessageView> thread) {
		final var attachments = new ArrayList<NumberedAttachment>();
		var number = 0;
		for (var messageIndex = 0; messageIndex < thread.size(); messageIndex++) {
			final var message = thread.get(messageIndex);
			final var role = role(message.direction());
			for (final var attachment : message.attachments()) {
				attachments.add(new NumberedAttachment(++number, messageIndex, role, message.created(), attachment.fileName(), attachment.mimeType(), attachment.content()));
			}
		}
		return attachments;
	}
}
