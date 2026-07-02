package se.sundsvall.caremanagement.journal.integration.db.model;

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

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand_journal_entry",
	indexes = {
		@Index(name = "idx_journal_entry_errand_id", columnList = "errand_id"),
		@Index(name = "idx_journal_entry_entry_date_time", columnList = "entry_date_time")
	})
public class JournalEntryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "entry_type", nullable = false, length = 255)
	private String type;

	@Column(name = "heading", nullable = false, length = 255)
	private String heading;

	@Column(name = "entry_text", length = 1_048_576)
	private String text;

	@Column(name = "entry_date_time", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime entryDateTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private JournalEntryStatus status;

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

	public static JournalEntryEntity create() {
		return new JournalEntryEntity();
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

	public OffsetDateTime getEntryDateTime() {
		return entryDateTime;
	}

	public JournalEntryStatus getStatus() {
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

	public void setEntryDateTime(final OffsetDateTime entryDateTime) {
		this.entryDateTime = entryDateTime;
	}

	public void setStatus(final JournalEntryStatus status) {
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

	public JournalEntryEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public JournalEntryEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public JournalEntryEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public JournalEntryEntity withHeading(final String heading) {
		this.heading = heading;
		return this;
	}

	public JournalEntryEntity withText(final String text) {
		this.text = text;
		return this;
	}

	public JournalEntryEntity withEntryDateTime(final OffsetDateTime entryDateTime) {
		this.entryDateTime = entryDateTime;
		return this;
	}

	public JournalEntryEntity withStatus(final JournalEntryStatus status) {
		this.status = status;
		return this;
	}

	public JournalEntryEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public JournalEntryEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public JournalEntryEntity withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public JournalEntryEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public JournalEntryEntity withLockedBy(final String lockedBy) {
		this.lockedBy = lockedBy;
		return this;
	}

	public JournalEntryEntity withLocked(final OffsetDateTime locked) {
		this.locked = locked;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final JournalEntryEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(type, other.type) && Objects.equals(heading, other.heading)
			&& Objects.equals(text, other.text) && Objects.equals(entryDateTime, other.entryDateTime)
			&& status == other.status
			&& Objects.equals(createdBy, other.createdBy) && Objects.equals(created, other.created)
			&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modified, other.modified)
			&& Objects.equals(lockedBy, other.lockedBy) && Objects.equals(locked, other.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, type, heading, text, entryDateTime, status, createdBy, created,
			modifiedBy, modified, lockedBy, locked);
	}

	@Override
	public String toString() {
		return "JournalEntryEntity{id='" + id + "', errandId='" + errandId + "', type='" + type + "', heading='"
			+ heading + "', entryDateTime=" + entryDateTime + ", status=" + status
			+ ", createdBy='" + createdBy + "', created=" + created + ", modifiedBy='" + modifiedBy + "', modified="
			+ modified + ", lockedBy='" + lockedBy + "', locked=" + locked + '}';
	}
}
