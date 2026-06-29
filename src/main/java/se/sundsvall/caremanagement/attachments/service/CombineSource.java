package se.sundsvall.caremanagement.attachments.service;

import java.util.Arrays;
import java.util.Objects;

/**
 * A single file to feed into {@link AttachmentService#combineToPdf(java.util.List)} — the cross-module input to the
 * platform's heterogeneous PDF combiner. PDFs are inlined as-is, images are rasterised onto a page, {@code .docx} is
 * rendered, and anything else becomes a one-page placeholder, so an odd file never blocks the merge.
 *
 * @param fileName the file name (used for type detection and the placeholder page)
 * @param mimeType the MIME type (used for type detection)
 * @param content  the raw file bytes
 */
public record CombineSource(String fileName, String mimeType, byte[] content) {

	@Override
	public boolean equals(final Object o) {
		return (this == o) || (o instanceof final CombineSource other
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
		return "CombineSource[fileName=%s, mimeType=%s, content=%d bytes]".formatted(fileName, mimeType, content == null ? 0 : content.length);
	}
}
