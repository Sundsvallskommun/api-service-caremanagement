package se.sundsvall.caremanagement.conversation.service.event;

import java.time.OffsetDateTime;

/**
 * Cross-module event published by the conversation module whenever a message is posted on an errand. Other modules can
 * subscribe to react — typically by sending the applicant a content-free notification (a "new message" notice pointing
 * to the citizen portal) when an OUTBOUND message (caseworker → applicant) is posted. The message body itself never
 * leaves the in-app thread; the event only signals that a new message exists.
 *
 * {@code direction} is INBOUND (applicant → caseworker) or OUTBOUND (caseworker → applicant); consumers typically act
 * only on OUTBOUND.
 */
public record MessagePosted(
	String messageId,
	String errandId,
	String direction,
	String author,
	OffsetDateTime timestamp) {}
