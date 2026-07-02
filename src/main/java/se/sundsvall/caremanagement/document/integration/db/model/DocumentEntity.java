package se.sundsvall.caremanagement.document.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
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
		@Index(name = "idx_document_document_date", columnList = "document_date")
	})
public class DocumentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "document_type", nullable = false, length = 255)
	private String type;

	@Column(name = "heading", nullable = false, length = 255)
	private String heading;

	@Column(name = "document_text", length = LONG32)
	private String text;

	@Column(name = "document_date", nullable = false)
	private LocalDate documentDate;

	@Column(name = "document_time")
	private LocalTime documentTime;

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

	public String getType() {
		return type;
	}

	public String getHeading() {
		return heading;
	}

	public String getText() {
		return text;
	}

	public LocalDate getDocumentDate() {
		return documentDate;
	}

	public LocalTime getDocumentTime() {
		return documentTime;
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

	public void setType(final String type) {
		this.type = type;
	}

	public void setHeading(final String heading) {
		this.heading = heading;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public void setDocumentDate(final LocalDate documentDate) {
		this.documentDate = documentDate;
	}

	public void setDocumentTime(final LocalTime documentTime) {
		this.documentTime = documentTime;
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

	public DocumentEntity withDocumentDate(final LocalDate documentDate) {
		this.documentDate = documentDate;
		return this;
	}

	public DocumentEntity withDocumentTime(final LocalTime documentTime) {
		this.documentTime = documentTime;
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

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final DocumentEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(type, other.type) && Objects.equals(heading, other.heading)
			&& Objects.equals(text, other.text) && Objects.equals(documentDate, other.documentDate)
			&& Objects.equals(documentTime, other.documentTime) && status == other.status
			&& Objects.equals(createdBy, other.createdBy) && Objects.equals(created, other.created)
			&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modified, other.modified)
			&& Objects.equals(lockedBy, other.lockedBy) && Objects.equals(locked, other.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, type, heading, text, documentDate, documentTime, status, createdBy, created,
			modifiedBy, modified, lockedBy, locked);
	}

	@Override
	public String toString() {
		return "DocumentEntity{id='" + id + "', errandId='" + errandId + "', type='" + type + "', heading='" + heading
			+ "', documentDate=" + documentDate + ", documentTime=" + documentTime + ", status=" + status
			+ ", createdBy='" + createdBy + "', created=" + created + ", modifiedBy='" + modifiedBy + "', modified="
			+ modified + ", lockedBy='" + lockedBy + "', locked=" + locked + '}';
	}
}
