package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single Lifecare document (metadata only) — the read model the frontend lists so a caseworker can see which
 * documents are registered on a person. The document body itself is not exposed.
 */
@Schema(description = "A Lifecare document, metadata only.")
public class LifecareDocument {

	@Schema(description = "The Lifecare document id", examples = "a3f1c2...")
	private String id;

	@Schema(description = "The document title", examples = "Beslut försörjningsstöd juni")
	private String title;

	@Schema(description = "The document date as Lifecare reports it", examples = "2026-06-01")
	private String date;

	@Schema(description = "The document type", examples = "Beslut")
	private String documentType;

	@Schema(description = "The id of the entity the document belongs to", examples = "9900")
	private String ownerId;

	@Schema(description = "The type of the entity the document belongs to", examples = "Decision")
	private String ownerType;

	public static LifecareDocument create() {
		return new LifecareDocument();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LifecareDocument withId(final String id) {
		this.id = id;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public LifecareDocument withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public LifecareDocument withDate(final String date) {
		this.date = date;
		return this;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(final String documentType) {
		this.documentType = documentType;
	}

	public LifecareDocument withDocumentType(final String documentType) {
		this.documentType = documentType;
		return this;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	public LifecareDocument withOwnerId(final String ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(final String ownerType) {
		this.ownerType = ownerType;
	}

	public LifecareDocument withOwnerType(final String ownerType) {
		this.ownerType = ownerType;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareDocument that = (LifecareDocument) o;
		return Objects.equals(id, that.id) && Objects.equals(title, that.title) && Objects.equals(date, that.date)
			&& Objects.equals(documentType, that.documentType) && Objects.equals(ownerId, that.ownerId) && Objects.equals(ownerType, that.ownerType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, title, date, documentType, ownerId, ownerType);
	}

	@Override
	public String toString() {
		return "LifecareDocument{id='" + id + "', title='" + title + "', date='" + date + "', documentType='" + documentType + "', ownerId='" + ownerId
			+ "', ownerType='" + ownerType + "'}";
	}
}
