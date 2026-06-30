package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.conversation.spi.ConversationMessageView;

/**
 * Flattens a conversation thread into a single, globally-numbered list of the attachments that get appended — the one
 * place attachment numbers are assigned, so the per-message listing in the rendered messages, the separator pages and
 * the appended order all use the same {@code Bilaga {n}}. Only applicant-sent (INBOUND) attachments are included:
 * handläggare-sent documents already exist in Lifecare, so they are listed on their message but never appended.
 * Numbering follows the thread: messages oldest-first, attachments in upload order within each message. The number, the
 * sender and the message date together distinguish two attachments that happen to share a file name.
 */
final class ThreadAttachments {

	static final String INBOUND = "INBOUND";
	static final String ROLE_APPLICANT = "Sökande";
	static final String ROLE_CASEWORKER = "Handläggare";

	private ThreadAttachments() {}

	/** Whether the applicant has sent at least one message — the precondition for archiving the conversation. */
	static boolean hasApplicantMessage(final List<ConversationMessageView> thread) {
		return thread.stream().anyMatch(message -> INBOUND.equals(message.direction()));
	}

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
	// Internal value carrier, built once in flatten() and consumed in order — never compared or hashed, so the default
	// record semantics are sufficient and content-aware equals/hashCode/toString (S6218) would be dead code.
	@SuppressWarnings("java:S6218")
	record NumberedAttachment(int number, int messageIndex, String role, OffsetDateTime created, String fileName, String mimeType, byte[] content) {}

	static String role(final String direction) {
		return INBOUND.equals(direction) ? ROLE_APPLICANT : ROLE_CASEWORKER;
	}

	/**
	 * The applicant-sent (INBOUND) attachments to append, numbered in thread order. Handläggare attachments are excluded.
	 */
	static List<NumberedAttachment> flatten(final List<ConversationMessageView> thread) {
		final var attachments = new ArrayList<NumberedAttachment>();
		var number = 0;
		for (var messageIndex = 0; messageIndex < thread.size(); messageIndex++) {
			final var message = thread.get(messageIndex);
			if (!INBOUND.equals(message.direction())) {
				continue;
			}
			for (final var attachment : message.attachments()) {
				attachments.add(new NumberedAttachment(++number, messageIndex, ROLE_APPLICANT, message.created(), attachment.fileName(), attachment.mimeType(), attachment.content()));
			}
		}
		return attachments;
	}
}
