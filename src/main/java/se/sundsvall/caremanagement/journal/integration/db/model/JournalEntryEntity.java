package se.sundsvall.caremanagement.journal.integration.db.model;

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
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand_journal_entry",
	indexes = {
		@Index(name = "idx_journal_entry_errand_id", columnList = "errand_id"),
		@Index(name = "idx_journal_entry_entry_date", columnList = "entry_date")
	})
public class JournalEntryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "entry_type", nullable = false, length = 255)
	private String type;

	@Column(name = "heading", nullable = false, length = 255)
	private String heading;

	@Column(name = "entry_text", length = LONG32)
	private String text;

	@Column(name = "entry_date", nullable = false)
	private LocalDate entryDate;

	@Column(name = "entry_time")
	private LocalTime entryTime;

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

	public LocalDate getEntryDate() {
		return entryDate;
	}

	public LocalTime getEntryTime() {
		return entryTime;
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

	public JournalEntryEntity withEntryDate(final LocalDate entryDate) {
		this.entryDate = entryDate;
		return this;
	}

	public JournalEntryEntity withEntryTime(final LocalTime entryTime) {
		this.entryTime = entryTime;
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
}
