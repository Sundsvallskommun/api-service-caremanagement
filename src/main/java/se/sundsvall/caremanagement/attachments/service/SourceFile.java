package se.sundsvall.caremanagement.attachments.service;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * A single file's content fully read into memory — an uploaded file, or a generated document such as a combined PDF —
 * so it can be persisted as an attachment and fed to {@link PdfCombiner} without re-reading an underlying stream.
 *
 * @param fileName    the file name (may be {@code null})
 * @param contentType the MIME type (may be {@code null})
 * @param content     the raw bytes (never {@code null})
 */
public record SourceFile(String fileName, String contentType, byte[] content) {

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
		return "SourceFile[fileName=%s, contentType=%s, content=%d bytes]".formatted(fileName, contentType, ofNullable(content).map(c -> c.length).orElse(0));
	}
}
