package se.sundsvall.caremanagement.conversation.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.sql.Blob;
import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * Binary content of a single {@link MessageAttachmentEntity}, kept in its own table and keyed by the attachment id (a
 * plain {@code message_attachment_id} column, no JPA back-reference) so the (potentially large) blob is only read when
 * the file is actually streamed. The DB-level FK to {@code message_attachment} carries {@code ON DELETE CASCADE}, so
 * the
 * blob row is removed atomically when its attachment — and transitively its message and errand — is deleted.
 */
@Entity
@Table(name = "message_attachment_data")
public class MessageAttachmentDataEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "message_attachment_id", nullable = false, length = 255)
	private String messageAttachmentId;

	@Column(name = "file", columnDefinition = "longblob")
	@Lob
	private Blob file;

	public static MessageAttachmentDataEntity create() {
		return new MessageAttachmentDataEntity();
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public MessageAttachmentDataEntity withId(final int id) {
		this.id = id;
		return this;
	}

	public String getMessageAttachmentId() {
		return messageAttachmentId;
	}

	public void setMessageAttachmentId(final String messageAttachmentId) {
		this.messageAttachmentId = messageAttachmentId;
	}

	public MessageAttachmentDataEntity withMessageAttachmentId(final String messageAttachmentId) {
		this.messageAttachmentId = messageAttachmentId;
		return this;
	}

	public Blob getFile() {
		return file;
	}

	public void setFile(final Blob file) {
		this.file = file;
	}

	public MessageAttachmentDataEntity withFile(final Blob file) {
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
		final MessageAttachmentDataEntity that = (MessageAttachmentDataEntity) o;
		return id == that.id && Objects.equals(messageAttachmentId, that.messageAttachmentId) && Objects.equals(file, that.file);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, messageAttachmentId, file);
	}

	@Override
	public String toString() {
		// The LOB itself is intentionally omitted from toString (debug noise / lazy proxy).
		return "MessageAttachmentDataEntity{id='" + id + "', messageAttachmentId='" + messageAttachmentId + "'}";
	}
}
