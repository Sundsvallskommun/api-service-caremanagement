package se.sundsvall.caremanagement.conversation.service.event;

import java.time.OffsetDateTime;

/**
 * Cross-module event published by the conversation module whenever a message is posted on an errand. Other modules can
 * subscribe to react — typically by sending the applicant a content-free notification (a "new message" notice pointing
 * to the citizen portal) when an OUTBOUND message (caseworker → applicant) is posted. The message body itself never
 * leaves the in-app thread; the event only signals that a new message exists.
 *
 * {@code direction} is INBOUND (applicant → caseworker) or OUTBOUND (caseworker → applicant); consumers typically act
 * only on OUTBOUND. {@code hasAttachments} lets a consumer cheaply skip messages with no files (e.g. the attachments
 * module only rebuilds the client-attachment PDF on INBOUND messages that actually carry attachments). The
 * {@code municipalityId}/{@code namespace} are carried so a consumer can act on the errand without re-deriving them.
 */
public record MessagePosted(
	String messageId,
	String municipalityId,
	String namespace,
	String errandId,
	String direction,
	String author,
	boolean hasAttachments,
	OffsetDateTime timestamp) {}
