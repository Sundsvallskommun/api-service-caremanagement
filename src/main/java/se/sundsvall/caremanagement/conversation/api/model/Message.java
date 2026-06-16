package se.sundsvall.caremanagement.conversation.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "A message in the errand's conversation")
public class Message {

	@Schema(description = "Unique identifier")
	private String id;

	@Schema(description = "The errand the message belongs to")
	private String errandId;

	@Schema(description = "Direction", allowableValues = {
		"INBOUND", "OUTBOUND"
	}, examples = "OUTBOUND")
	private String direction;

	@Schema(description = "Message text", examples = "Please complete your application with a valid certificate.")
	private String body;

	@Schema(description = "Author id", examples = "joe01doe")
	private String author;

	@Schema(description = "Created timestamp")
	private OffsetDateTime created;

	@Schema(description = "Files attached to the message", accessMode = READ_ONLY)
	private List<MessageAttachment> attachments = new ArrayList<>();

	public static Message create() {
		return new Message();
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

	public OffsetDateTime getCreated() {
		return created;
	}

	public List<MessageAttachment> getAttachments() {
		return attachments;
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

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public void setAttachments(final List<MessageAttachment> attachments) {
		this.attachments = attachments;
	}

	public Message withId(final String id) {
		this.id = id;
		return this;
	}

	public Message withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public Message withDirection(final String direction) {
		this.direction = direction;
		return this;
	}

	public Message withBody(final String body) {
		this.body = body;
		return this;
	}

	public Message withAuthor(final String author) {
		this.author = author;
		return this;
	}

	public Message withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public Message withAttachments(final List<MessageAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Message that = (Message) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(direction, that.direction)
			&& Objects.equals(body, that.body) && Objects.equals(author, that.author) && Objects.equals(created, that.created)
			&& Objects.equals(attachments, that.attachments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, direction, body, author, created, attachments);
	}

	@Override
	public String toString() {
		return "Message{id='" + id + "', errandId='" + errandId + "', direction='" + direction + "', body='" + body
			+ "', author='" + author + "', created=" + created + ", attachments=" + attachments + '}';
	}
}
