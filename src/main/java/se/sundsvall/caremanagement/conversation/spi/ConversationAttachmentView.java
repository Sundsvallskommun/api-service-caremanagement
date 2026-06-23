package se.sundsvall.caremanagement.conversation.spi;

/**
 * Read-only cross-module view of a single conversation attachment, including its binary content, for archiving the
 * thread into a document. The content is fully materialised to {@code byte[]} inside the conversation module's own
 * transaction so no {@code Blob}/stream crosses the boundary.
 *
 * @param fileName the original file name
 * @param mimeType the MIME type
 * @param content  the raw file bytes
 */
public record ConversationAttachmentView(String fileName, String mimeType, byte[] content) {
}
