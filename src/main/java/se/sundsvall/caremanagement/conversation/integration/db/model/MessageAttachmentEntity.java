package se.sundsvall.caremanagement.conversation.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Metadata of a file attached to a conversation {@link MessageEntity} — scoped to its message (which is in turn scoped
 * to an errand). The binary content lives in a separate {@link MessageAttachmentDataEntity} row keyed by this row's id,
 * so loading attachment metadata (e.g. when listing a thread) never pulls the blob into memory.
 */
@Entity
@Table(name = "message_attachment",
	indexes = {
		@Index(name = "idx_message_attachment_message_id", columnList = "message_id")
	})
public class MessageAttachmentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "message_id", nullable = false, length = 255)
	private String messageId;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "file_size")
	private Integer fileSize;

	@Column(name = "sender_role", length = 32)
	private String senderRole;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static MessageAttachmentEntity create() {
		return new MessageAttachmentEntity();
	}

	public String getId() {
		return id;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public String getSenderRole() {
		return senderRole;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setId(final String v) {
		this.id = v;
	}

	public void setMessageId(final String v) {
		this.messageId = v;
	}

	public void setFileName(final String v) {
		this.fileName = v;
	}

	public void setMimeType(final String v) {
		this.mimeType = v;
	}

	public void setFileSize(final Integer v) {
		this.fileSize = v;
	}

	public void setSenderRole(final String v) {
		this.senderRole = v;
	}

	public void setCreated(final OffsetDateTime v) {
		this.created = v;
	}

	public MessageAttachmentEntity withId(final String v) {
		this.id = v;
		return this;
	}

	public MessageAttachmentEntity withMessageId(final String v) {
		this.messageId = v;
		return this;
	}

	public MessageAttachmentEntity withFileName(final String v) {
		this.fileName = v;
		return this;
	}

	public MessageAttachmentEntity withMimeType(final String v) {
		this.mimeType = v;
		return this;
	}

	public MessageAttachmentEntity withFileSize(final Integer v) {
		this.fileSize = v;
		return this;
	}

	public MessageAttachmentEntity withSenderRole(final String v) {
		this.senderRole = v;
		return this;
	}

	public MessageAttachmentEntity withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageAttachmentEntity that = (MessageAttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(messageId, that.messageId)
			&& Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType)
			&& Objects.equals(fileSize, that.fileSize) && Objects.equals(senderRole, that.senderRole)
			&& Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, messageId, fileName, mimeType, fileSize, senderRole, created);
	}

	@Override
	public String toString() {
		return "MessageAttachmentEntity{id='" + id + "', messageId='" + messageId + "', fileName='" + fileName
			+ "', mimeType='" + mimeType + "', fileSize=" + fileSize + ", senderRole='" + senderRole + "', created=" + created + '}';
	}
}
