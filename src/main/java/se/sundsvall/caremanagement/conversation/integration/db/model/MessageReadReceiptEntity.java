package se.sundsvall.caremanagement.conversation.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Records that a reader side ({@code CASEWORKER} or {@code CLIENT}) has read a specific message. A side's unread count
 * is the messages addressed to it (the opposite {@code direction}) that have no receipt for that side. The
 * {@code (message_id, reader_side)} pair is unique, so reading a message twice is idempotent.
 */
@Entity
@Table(name = "message_read_receipt",
	uniqueConstraints = @UniqueConstraint(name = "uk_message_read_receipt_message_side", columnNames = {
		"message_id", "reader_side"
	}),
	indexes = {
		@Index(name = "idx_message_read_receipt_message_id", columnList = "message_id")
	})
public class MessageReadReceiptEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "message_id", nullable = false, length = 255)
	private String messageId;

	@Column(name = "reader_side", nullable = false, length = 16)
	private String readerSide;

	@Column(name = "read_by", length = 64)
	private String readBy;

	@Column(name = "read_at", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime readAt;

	public static MessageReadReceiptEntity create() {
		return new MessageReadReceiptEntity();
	}

	public String getId() {
		return id;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getReaderSide() {
		return readerSide;
	}

	public String getReadBy() {
		return readBy;
	}

	public OffsetDateTime getReadAt() {
		return readAt;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setMessageId(final String messageId) {
		this.messageId = messageId;
	}

	public void setReaderSide(final String readerSide) {
		this.readerSide = readerSide;
	}

	public void setReadBy(final String readBy) {
		this.readBy = readBy;
	}

	public void setReadAt(final OffsetDateTime readAt) {
		this.readAt = readAt;
	}

	public MessageReadReceiptEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public MessageReadReceiptEntity withMessageId(final String messageId) {
		this.messageId = messageId;
		return this;
	}

	public MessageReadReceiptEntity withReaderSide(final String readerSide) {
		this.readerSide = readerSide;
		return this;
	}

	public MessageReadReceiptEntity withReadBy(final String readBy) {
		this.readBy = readBy;
		return this;
	}

	public MessageReadReceiptEntity withReadAt(final OffsetDateTime readAt) {
		this.readAt = readAt;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageReadReceiptEntity that = (MessageReadReceiptEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(messageId, that.messageId)
			&& Objects.equals(readerSide, that.readerSide) && Objects.equals(readBy, that.readBy)
			&& Objects.equals(readAt, that.readAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, messageId, readerSide, readBy, readAt);
	}

	@Override
	public String toString() {
		return "MessageReadReceiptEntity{id='" + id + "', messageId='" + messageId + "', readerSide='" + readerSide
			+ "', readBy='" + readBy + "', readAt=" + readAt + '}';
	}
}
