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

@Entity
@Table(name = "errand_message",
	indexes = {
		@Index(name = "idx_message_errand_id", columnList = "errand_id"),
		@Index(name = "idx_message_created", columnList = "created")
	})
public class MessageEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "direction", nullable = false, length = 16)
	private String direction;

	@Column(name = "body", nullable = false, length = 8192)
	private String body;

	@Column(name = "author", length = 64)
	private String author;

	@Column(name = "in_reply_to_id", length = 36)
	private String inReplyToId;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static MessageEntity create() {
		return new MessageEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getDirection() {
		return direction;
	}

	public String getBody() {
		return body;
	}

	public String getAuthor() {
		return author;
	}

	public String getInReplyToId() {
		return inReplyToId;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setDirection(final String direction) {
		this.direction = direction;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	public void setInReplyToId(final String inReplyToId) {
		this.inReplyToId = inReplyToId;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MessageEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public MessageEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public MessageEntity withDirection(final String direction) {
		this.direction = direction;
		return this;
	}

	public MessageEntity withBody(final String body) {
		this.body = body;
		return this;
	}

	public MessageEntity withAuthor(final String author) {
		this.author = author;
		return this;
	}

	public MessageEntity withInReplyToId(final String inReplyToId) {
		this.inReplyToId = inReplyToId;
		return this;
	}

	public MessageEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageEntity that = (MessageEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(direction, that.direction) && Objects.equals(body, that.body)
			&& Objects.equals(author, that.author) && Objects.equals(inReplyToId, that.inReplyToId)
			&& Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, direction, body, author, inReplyToId, created);
	}

	@Override
	public String toString() {
		return "MessageEntity{id='" + id + "', errandId='" + errandId + "', direction='" + direction + "', body='" + body
			+ "', author='" + author + "', inReplyToId='" + inReplyToId + "', created=" + created + '}';
	}
}
