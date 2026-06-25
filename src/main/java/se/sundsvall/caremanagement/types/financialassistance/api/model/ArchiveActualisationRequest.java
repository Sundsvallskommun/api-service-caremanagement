package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Optional metadata for archiving an uploaded document (e.g. a supplementary application — tilläggsansökan) to a
 * Lifecare actualisation. Sent as the JSON {@code request} part alongside the file in the multipart archive call. Every
 * field is optional; an omitted field falls back to a server default (and the title defaults to the uploaded file
 * name). {@code documentType} and {@code documentSenderType} are Lifecare {@code InsertDocumentType} /
 * {@code InsertDocumentSenderType} codes — set them when the default does not match the document being archived. When
 * {@code errandId} is present, the target actualisation id is recorded on that errand as a
 * {@code Decision(ACTUALISATION)} so the errand now points at the actualisation the document was archived to.
 */
@Schema(description = "Optional metadata for archiving a document to a Lifecare actualisation.")
public class ArchiveActualisationRequest {

	@Schema(
		description = "The id of the caremanagement errand the archive concerns. When present, the target actualisation id is recorded on the errand as a Decision(ACTUALISATION) — setting the errand's Lifecare actualisation to the one archived to.",
		examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	@ValidUuid(nullable = true)
	private String errandId;

	@Schema(description = "The document title shown in Lifecare. Defaults to the uploaded file name when omitted.", examples = "Tilläggsansökan")
	private String title;

	@Schema(description = "The Lifecare InsertDocumentType code for the document. Server default when omitted.", examples = "ANSOKAN")
	private String documentType;

	@Schema(description = "The Lifecare InsertDocumentSenderType code for the document. Server default when omitted.", examples = "ENSKILD")
	private String documentSenderType;

	@Schema(description = "The sender name shown in Lifecare. Server default when omitted.", examples = "Draken")
	private String senderName;

	public static ArchiveActualisationRequest create() {
		return new ArchiveActualisationRequest();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public ArchiveActualisationRequest withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public ArchiveActualisationRequest withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(final String documentType) {
		this.documentType = documentType;
	}

	public ArchiveActualisationRequest withDocumentType(final String documentType) {
		this.documentType = documentType;
		return this;
	}

	public String getDocumentSenderType() {
		return documentSenderType;
	}

	public void setDocumentSenderType(final String documentSenderType) {
		this.documentSenderType = documentSenderType;
	}

	public ArchiveActualisationRequest withDocumentSenderType(final String documentSenderType) {
		this.documentSenderType = documentSenderType;
		return this;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(final String senderName) {
		this.senderName = senderName;
	}

	public ArchiveActualisationRequest withSenderName(final String senderName) {
		this.senderName = senderName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ArchiveActualisationRequest that = (ArchiveActualisationRequest) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(title, that.title) && Objects.equals(documentType, that.documentType)
			&& Objects.equals(documentSenderType, that.documentSenderType) && Objects.equals(senderName, that.senderName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, title, documentType, documentSenderType, senderName);
	}

	@Override
	public String toString() {
		return "ArchiveActualisationRequest{errandId='" + errandId + "', title='" + title + "', documentType='" + documentType + "', documentSenderType='"
			+ documentSenderType + "', senderName='" + senderName + "'}";
	}
}
