package se.sundsvall.caremanagement.notes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "Note attached to an errand")
public class Note {

	@Schema(description = "Unique identifier")
	private String id;

	@Schema(description = "Errand id this note belongs to")
	private String errandId;

	@Schema(description = "Note body", example = "Spoke to family today, awaiting docs.")
	private String body;

	@Schema(description = "Author user id", example = "jane01doe")
	private String author;

	@Schema(description = "Created timestamp")
	private OffsetDateTime created;

	public static Note create() {
		return new Note();
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

	public void setId(final String v) {
		this.id = v;
	}

	public void setErrandId(final String v) {
		this.errandId = v;
	}

	public void setBody(final String v) {
		this.body = v;
	}

	public void setAuthor(final String v) {
		this.author = v;
	}

	public void setCreated(final OffsetDateTime v) {
		this.created = v;
	}

	public Note withId(final String v) {
		this.id = v;
		return this;
	}

	public Note withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public Note withBody(final String v) {
		this.body = v;
		return this;
	}

	public Note withAuthor(final String v) {
		this.author = v;
		return this;
	}

	public Note withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Note that = (Note) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(body, that.body) && Objects.equals(author, that.author)
			&& Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, body, author, created);
	}
}
