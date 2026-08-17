package se.sundsvall.caremanagement.lifecare.integration;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The metadata + bytes of a document uploaded and bound to a Lifecare actualisation. Grouped into one carrier so
 * {@link LifecareFamilyCareIntegration#postActualisationAttachment(Integer, ActualisationAttachment)} stays under the
 * parameter
 * limit. Carries no personal identity number — the document is bound by {@code actualisationId} only.
 *
 * @param documentType       the Lifecare {@code InsertDocumentType} code
 * @param documentSenderType the Lifecare {@code InsertDocumentSenderType} code
 * @param title              the document title
 * @param senderName         the sender name
 * @param fileName           the file name shown in Lifecare
 * @param mimeType           the document MIME type
 * @param content            the raw document bytes
 */
public record ActualisationAttachment(
	String documentType,
	String documentSenderType,
	String title,
	String senderName,
	String fileName,
	String mimeType,
	byte[] content) {

	@Override
	public boolean equals(final Object o) {
		return (this == o) || (o instanceof final ActualisationAttachment other
			&& Objects.equals(documentType, other.documentType)
			&& Objects.equals(documentSenderType, other.documentSenderType)
			&& Objects.equals(title, other.title)
			&& Objects.equals(senderName, other.senderName)
			&& Objects.equals(fileName, other.fileName)
			&& Objects.equals(mimeType, other.mimeType)
			&& Arrays.equals(content, other.content));
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentType, documentSenderType, title, senderName, fileName, mimeType, Arrays.hashCode(content));
	}

	@Override
	public String toString() {
		return "ActualisationAttachment[documentType=%s, documentSenderType=%s, title=%s, senderName=%s, fileName=%s, mimeType=%s, content=%d bytes]"
			.formatted(documentType, documentSenderType, title, senderName, fileName, mimeType, ofNullable(content).map(c -> c.length).orElse(0));
	}
}
