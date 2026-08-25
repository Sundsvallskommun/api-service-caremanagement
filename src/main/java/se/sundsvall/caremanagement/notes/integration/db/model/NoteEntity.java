package se.sundsvall.caremanagement.notes.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "errand_note",
	indexes = {
		@Index(name = "idx_note_errand_id", columnList = "errand_id"),
		@Index(name = "idx_note_created", columnList = "created")
	})
public class NoteEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "body", nullable = false, length = LONG32)
	private String body;

	@Column(name = "author", length = 64)
	private String author;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified_by", length = 64)
	private String modifiedBy;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static NoteEntity create() {
		return new NoteEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getBody() {
		return body;
	}

	public String getAuthor() {
		return author;
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

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public void setAuthor(final String author) {
		this.author = author;
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

	public NoteEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public NoteEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public NoteEntity withBody(final String body) {
		this.body = body;
		return this;
	}

	public NoteEntity withAuthor(final String author) {
		this.author = author;
		return this;
	}

	public NoteEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public NoteEntity withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public NoteEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	// 'body' (a LONG32 column) is deliberately excluded from equals/hashCode/toString — it can be large and is not part of
	// the entity's identity.
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final NoteEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(author, other.author)
			&& Objects.equals(created, other.created) && Objects.equals(modifiedBy, other.modifiedBy)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, author, created, modifiedBy, modified);
	}

	@Override
	public String toString() {
		return "NoteEntity{id='" + id + "', errandId='" + errandId + "', author='" + author
			+ "', created=" + created + ", modifiedBy='" + modifiedBy + "', modified=" + modified + '}';
	}
}
