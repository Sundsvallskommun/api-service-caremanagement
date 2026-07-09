package se.sundsvall.caremanagement.notes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "Note attached to an errand")
public class Note {

	@Schema(description = "Unique identifier")
	private String id;

	@Schema(description = "Errand id this note belongs to")
	private String errandId;

	@Schema(description = "Note body", examples = "Spoke to family today, awaiting docs.")
	private String body;

	@Schema(description = "Author user id", examples = "jane01doe")
	private String author;

	@Schema(description = "Created timestamp")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "User id of the last editor", examples = "jane01doe")
	private String modifiedBy;

	@Schema(description = "Last modified timestamp; null until the note has been edited")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

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

	public Note withId(final String id) {
		this.id = id;
		return this;
	}

	public Note withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public Note withBody(final String body) {
		this.body = body;
		return this;
	}

	public Note withAuthor(final String author) {
		this.author = author;
		return this;
	}

	public Note withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public Note withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Note withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Note that = (Note) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(body, that.body) && Objects.equals(author, that.author)
			&& Objects.equals(created, that.created) && Objects.equals(modifiedBy, that.modifiedBy)
			&& Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, body, author, created, modifiedBy, modified);
	}
}
