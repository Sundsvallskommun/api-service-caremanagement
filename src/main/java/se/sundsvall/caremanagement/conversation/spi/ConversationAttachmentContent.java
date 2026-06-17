package se.sundsvall.caremanagement.conversation.spi;

/**
 * A single conversation attachment with its binary content fully read into memory, for feeding to the PDF combiner.
 * Materialised inside the conversation module's transaction so no {@code Blob}/stream escapes the module boundary.
 */
public record ConversationAttachmentContent(
	String fileName,
	String mimeType,
	byte[] content) {}
