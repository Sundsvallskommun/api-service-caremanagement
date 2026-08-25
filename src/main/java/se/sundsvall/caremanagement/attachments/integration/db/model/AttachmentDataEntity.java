package se.sundsvall.caremanagement.attachments.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Blob;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "attachment_data")
public class AttachmentDataEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "file", columnDefinition = "longblob")
	@Lob
	private Blob file;

	public static AttachmentDataEntity create() {
		return new AttachmentDataEntity();
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public AttachmentDataEntity withId(final int id) {
		this.id = id;
		return this;
	}

	public Blob getFile() {
		return file;
	}

	public void setFile(final Blob file) {
		this.file = file;
	}

	public AttachmentDataEntity withFile(final Blob file) {
		this.file = file;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final AttachmentDataEntity that = (AttachmentDataEntity) o;
		// Identity is the persisted id only. The blob is deliberately excluded (comparing it would read the whole
		// longblob), and a transient row (id 0, pre-persist) is equal only to itself — never to another unsaved row.
		return id != 0 && id == that.id;
	}

	@Override
	public int hashCode() {
		// Constant, class-based hash: stable across the entity's lifecycle — the generated id is 0 until persisted, so an
		// id-based hash would change on save and break HashSet membership — and it never reads the blob.
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		// The LOB itself is intentionally omitted from toString (debug noise / lazy proxy).
		return "AttachmentDataEntity{id='" + id + "'}";
	}
}
