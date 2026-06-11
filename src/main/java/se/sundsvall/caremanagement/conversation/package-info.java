/**
 * Conversation module — structured messages between caseworker and applicant on an errand.
 *
 * Universal across all errand types: a message is just {@code (errandId, direction, body, author, created)}. OUTBOUND =
 * caseworker → applicant (e.g. a request for complementary information shown in the citizen portal), INBOUND =
 * applicant → caseworker (in-app reply). Persists the thread and publishes a {@code MessagePosted} event; other modules
 * subscribe to notify — typically a content-free notification on OUTBOUND, so the message body itself never leaves this
 * in-app thread.
 */
@ApplicationModule(displayName = "Conversation")
package se.sundsvall.caremanagement.conversation;

import org.springframework.modulith.ApplicationModule;
