package se.sundsvall.caremanagement.lifecare.service;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A document to attach to an actualisation: the two Lifecare {@code InsertDocument*} type codes, the title and
 * sender shown in Lifecare, and the PDF itself.
 *
 * <p>
 * Lives in the exposed {@code lifecare.service} named interface (not {@code lifecare.integration}) so type modules
 * such as {@code types.financialassistance} can pass one to {@link ActualisationService#uploadAttachment} without
 * reaching across the module boundary into the integration layer (a Spring Modulith violation).
 */
public record AttachmentUpload(String documentType, String documentSenderType, String title, String senderName, String fileName, byte[] content) {

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final AttachmentUpload other)) {
			return false;
		}
		return Objects.equals(documentType, other.documentType)
			&& Objects.equals(documentSenderType, other.documentSenderType)
			&& Objects.equals(title, other.title)
			&& Objects.equals(senderName, other.senderName)
			&& Objects.equals(fileName, other.fileName)
			&& Arrays.equals(content, other.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentType, documentSenderType, title, senderName, fileName, Arrays.hashCode(content));
	}

	/** Summarizes {@code content} by its length: PDF bytes must never end up in a log line via a careless toString(). */
	@Override
	public String toString() {
		return "AttachmentUpload[documentType=%s, documentSenderType=%s, title=%s, senderName=%s, fileName=%s, content=%d bytes]"
			.formatted(documentType, documentSenderType, title, senderName, fileName, Optional.ofNullable(content).map(bytes -> bytes.length).orElse(0));
	}
}
