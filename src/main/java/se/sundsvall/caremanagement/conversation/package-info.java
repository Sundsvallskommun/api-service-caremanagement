/**
 * Conversation module — structured messages between caseworker and applicant on an errand.
 *
 * Universal across all errand types: a message is {@code (errandId, direction, body, author, created)} plus any number
 * of file attachments. OUTBOUND = caseworker → applicant (e.g. a request for complementary information shown in the
 * citizen portal), INBOUND = applicant → caseworker (in-app reply). Persists the thread and publishes a
 * {@code MessagePosted} event; other modules subscribe to notify — typically a content-free notification on OUTBOUND,
 * so
 * the message body and attachments themselves never leave this in-app thread. Attachment content is self-contained in
 * the module (its own {@code message_attachment} / {@code message_attachment_data} tables) and cleaned up by DB-level
 * cascade when the message — and transitively the errand — is deleted.
 */
@ApplicationModule(displayName = "Conversation")
package se.sundsvall.caremanagement.conversation;

import org.springframework.modulith.ApplicationModule;
