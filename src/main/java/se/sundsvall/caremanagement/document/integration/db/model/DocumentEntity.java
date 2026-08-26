package se.sundsvall.caremanagement.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand_document",
	indexes = {
		@Index(name = "idx_document_errand_id", columnList = "errand_id"),
		@Index(name = "idx_document_document_date_time", columnList = "document_date_time"),
		// Unique: lifecareId is the idempotency key the RPA supplements ingest upserts on — a duplicate pair would break
		// the upsert lookup.
		@Index(name = "uq_document_errand_id_lifecare_id", columnList = "errand_id, lifecare_id", unique = true)
	})
public class DocumentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "source", length = 16)
	private String source;

	@Column(name = "lifecare_id", length = 64)
	private String lifecareId;

	@Column(name = "document_type", nullable = false, length = 255)
	private String type;

	@Column(name = "heading", nullable = false, length = 255)
	private String heading;

	@Column(name = "document_text", length = LONG32)
	private String text;

	@Column(name = "document_date_time", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime documentDateTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private DocumentStatus status;

	@Column(name = "created_by", length = 64)
	private String createdBy;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified_by", length = 64)
	private String modifiedBy;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "locked_by", length = 64)
	private String lockedBy;

	@Column(name = "locked")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime locked;

	public static DocumentEntity create() {
		return new DocumentEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getSource() {
		return source;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public String getType() {
		return type;
	}

	public String getHeading() {
		return heading;
	}

	public String getText() {
		return text;
	}

	public OffsetDateTime getDocumentDateTime() {
		return documentDateTime;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public String getLockedBy() {
		return lockedBy;
	}

	public OffsetDateTime getLocked() {
		return locked;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setHeading(final String heading) {
		this.heading = heading;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public void setDocumentDateTime(final OffsetDateTime documentDateTime) {
		this.documentDateTime = documentDateTime;
	}

	public void setStatus(final DocumentStatus status) {
		this.status = status;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public void setLockedBy(final String lockedBy) {
		this.lockedBy = lockedBy;
	}

	public void setLocked(final OffsetDateTime locked) {
		this.locked = locked;
	}

	public DocumentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public DocumentEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public DocumentEntity withSource(final String source) {
		this.source = source;
		return this;
	}

	public DocumentEntity withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public DocumentEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public DocumentEntity withHeading(final String heading) {
		this.heading = heading;
		return this;
	}

	public DocumentEntity withText(final String text) {
		this.text = text;
		return this;
	}

	public DocumentEntity withDocumentDateTime(final OffsetDateTime documentDateTime) {
		this.documentDateTime = documentDateTime;
		return this;
	}

	public DocumentEntity withStatus(final DocumentStatus status) {
		this.status = status;
		return this;
	}

	public DocumentEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public DocumentEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public DocumentEntity withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public DocumentEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public DocumentEntity withLockedBy(final String lockedBy) {
		this.lockedBy = lockedBy;
		return this;
	}

	public DocumentEntity withLocked(final OffsetDateTime locked) {
		this.locked = locked;
		return this;
	}

	// 'text' (the document body, a LONG32 column) is deliberately excluded from equals/hashCode and toString — it can
	// be large, it is not part of the document's identity, and hashing it on every lookup would be wasteful.
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final DocumentEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(source, other.source) && Objects.equals(lifecareId, other.lifecareId)
			&& Objects.equals(type, other.type) && Objects.equals(heading, other.heading)
			&& Objects.equals(documentDateTime, other.documentDateTime)
			&& status == other.status
			&& Objects.equals(createdBy, other.createdBy) && Objects.equals(created, other.created)
			&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modified, other.modified)
			&& Objects.equals(lockedBy, other.lockedBy) && Objects.equals(locked, other.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, source, lifecareId, type, heading, documentDateTime, status, createdBy, created,
			modifiedBy, modified, lockedBy, locked);
	}

	@Override
	public String toString() {
		return "DocumentEntity{id='" + id + "', errandId='" + errandId + "', source='" + source + "', lifecareId='" + lifecareId + "', type='" + type + "', heading='" + heading
			+ "', documentDateTime=" + documentDateTime + ", status=" + status
			+ ", createdBy='" + createdBy + "', created=" + created + ", modifiedBy='" + modifiedBy + "', modified="
			+ modified + ", lockedBy='" + lockedBy + "', locked=" + locked + '}';
	}
}
