package se.sundsvall.caremanagement.lifecare.integration;

/**
 * The metadata + bytes of a document uploaded and bound to a Lifecare actualisation. Grouped into one carrier so
 * {@link LifecareFcIntegration#postActualisationAttachment(Integer, ActualisationAttachment)} stays under the parameter
 * limit. Carries no personnummer — the document is bound by {@code actualisationId} only.
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
}
