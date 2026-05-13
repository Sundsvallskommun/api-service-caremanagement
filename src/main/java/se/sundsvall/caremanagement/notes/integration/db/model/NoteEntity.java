package se.sundsvall.caremanagement.notes.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "body", nullable = false, length = LONG32)
	private String body;

	@Column(name = "author", length = 64)
	private String author;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

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

	public NoteEntity withId(final String v) {
		this.id = v;
		return this;
	}

	public NoteEntity withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public NoteEntity withBody(final String v) {
		this.body = v;
		return this;
	}

	public NoteEntity withAuthor(final String v) {
		this.author = v;
		return this;
	}

	public NoteEntity withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}
}
