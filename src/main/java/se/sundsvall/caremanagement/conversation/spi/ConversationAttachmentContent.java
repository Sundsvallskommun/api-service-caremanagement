package se.sundsvall.caremanagement.conversation.spi;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * A single conversation attachment with its binary content fully read into memory, for feeding to the PDF combiner.
 * Materialised inside the conversation module's transaction so no {@code Blob}/stream escapes the module boundary.
 */
public record ConversationAttachmentContent(
	String fileName,
	String mimeType,
	byte[] content) {

	@Override
	public boolean equals(final Object o) {
		return (this == o) || (o instanceof final ConversationAttachmentContent other
			&& Objects.equals(fileName, other.fileName)
			&& Objects.equals(mimeType, other.mimeType)
			&& Arrays.equals(content, other.content));
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileName, mimeType, Arrays.hashCode(content));
	}

	@Override
	public String toString() {
		return "ConversationAttachmentContent[fileName=%s, mimeType=%s, content=%d bytes]".formatted(fileName, mimeType, ofNullable(content).map(c -> c.length).orElse(0));
	}
}
