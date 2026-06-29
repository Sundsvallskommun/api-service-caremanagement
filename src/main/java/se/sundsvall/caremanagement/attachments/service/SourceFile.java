package se.sundsvall.caremanagement.attachments.service;

import java.util.Arrays;
import java.util.Objects;

/**
 * A single uploaded file read fully into memory once, so it can be both persisted as an attachment and fed to
 * {@link PdfCombiner} without re-reading the underlying multipart stream.
 *
 * @param fileName    the original filename (may be {@code null})
 * @param contentType the reported MIME type (may be {@code null})
 * @param content     the raw bytes (never {@code null})
 */
record SourceFile(String fileName, String contentType, byte[] content) {

	@Override
	public boolean equals(final Object o) {
		return (this == o) || (o instanceof final SourceFile other
			&& Objects.equals(fileName, other.fileName)
			&& Objects.equals(contentType, other.contentType)
			&& Arrays.equals(content, other.content));
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileName, contentType, Arrays.hashCode(content));
	}

	@Override
	public String toString() {
		return "SourceFile[fileName=%s, contentType=%s, content=%d bytes]".formatted(fileName, contentType, content == null ? 0 : content.length);
	}
}
