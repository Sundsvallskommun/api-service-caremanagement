package se.sundsvall.caremanagement.conversation.spi;

import java.time.OffsetDateTime;

/**
 * Metadata of a single conversation attachment, for inclusion in the unified errand attachment list. Carries no binary
 * content — the file is downloaded through the conversation endpoint
 * ({@code .../messages/{messageId}/attachments/{id}/file}), so {@code messageId} is included to let the caller build
 * that URL.
 *
 * @param senderRole CLIENT (the applicant sent it, INBOUND) or HANDLAGGARE (the caseworker sent it, OUTBOUND)
 */
public record ConversationAttachment(
	String id,
	String messageId,
	String fileName,
	String mimeType,
	Integer fileSize,
	OffsetDateTime created,
	String senderRole) {}
