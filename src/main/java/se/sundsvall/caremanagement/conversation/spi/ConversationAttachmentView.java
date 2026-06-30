package se.sundsvall.caremanagement.conversation.spi;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Optional.ofNullable;

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

	@Override
	public boolean equals(final Object o) {
		return (this == o) || (o instanceof final ConversationAttachmentView other
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
		return "ConversationAttachmentView[fileName=%s, mimeType=%s, content=%d bytes]".formatted(fileName, mimeType, ofNullable(content).map(c -> c.length).orElse(0));
	}
}
