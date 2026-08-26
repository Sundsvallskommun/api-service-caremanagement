package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * A document (formal case document) attached to an errand — the Lifecare document shape, captured so it can later be
 * RPA:ed into Lifecare.
 */
@Schema(description = "A document (formal case document) attached to an errand")
public class Document {

	@Schema(description = "Unique identifier")
	private String id;

	@Schema(description = "Errand id this document belongs to")
	private String errandId;

	@Schema(description = "Provenance — CASEWORKER for a document authored in Draken, LIFECARE for one read out of Lifecare by RPA and mirrored onto the errand", examples = "CASEWORKER")
	private String source;

	@Schema(description = "The document's id in Lifecare's document list — set on LIFECARE-sourced mirrors (the RPA upsert key)", examples = "27")
	private String lifecareId;

	@Schema(description = "Document type (Lifecare 'Typ'/Dokumenttyp). A municipality-configured value; see the metadata catalogue for a provisional set.", examples = "Brev")
	private String type;

	@Schema(description = "Heading (Lifecare 'Rubrik')", examples = "Beslut om ekonomiskt bistånd 2025-05")
	private String heading;

	@Schema(description = "Free-text body of the document", examples = "Beslut har fattats enligt nedan ...")
	private String text;

	@Schema(description = "Documented date and time (Lifecare 'Datum'/'Tid'), distinct from the system created timestamp", examples = "2025-05-30T14:30:00+02:00")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime documentDateTime;

	@Schema(description = "Write-protection status — WORKING is an editable draft, LOCKED is a finalised record", allowableValues = {
		"WORKING", "LOCKED"
	}, examples = "WORKING")
	private String status;

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'/'Ägare')", examples = "carola01winberg")
	private String createdBy;

	@Schema(description = "Created timestamp")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "User id of the last editor (Lifecare 'Ändrat av'); null until the document has been edited", examples = "ebb14eri")
	private String modifiedBy;

	@Schema(description = "Last modified timestamp; null until the document has been edited")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "User id of whoever locked the document; null while WORKING", examples = "carola01winberg")
	private String lockedBy;

	@Schema(description = "Timestamp when the document was locked (became an upprättad handling); null while WORKING")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime locked;

	public static Document create() {
		return new Document();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getSource() {
		return source;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public String getType() {
		return type;
	}

	public String getHeading() {
		return heading;
	}

	public String getText() {
		return text;
	}

	public OffsetDateTime getDocumentDateTime() {
		return documentDateTime;
	}

	public String getStatus() {
		return status;
	}

	public String getCreatedBy() {
		return createdBy;
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

	public String getLockedBy() {
		return lockedBy;
	}

	public OffsetDateTime getLocked() {
		return locked;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setHeading(final String heading) {
		this.heading = heading;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public void setDocumentDateTime(final OffsetDateTime documentDateTime) {
		this.documentDateTime = documentDateTime;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
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

	public void setLockedBy(final String lockedBy) {
		this.lockedBy = lockedBy;
	}

	public void setLocked(final OffsetDateTime locked) {
		this.locked = locked;
	}

	public Document withId(final String id) {
		this.id = id;
		return this;
	}

	public Document withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public Document withSource(final String source) {
		this.source = source;
		return this;
	}

	public Document withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public Document withType(final String type) {
		this.type = type;
		return this;
	}

	public Document withHeading(final String heading) {
		this.heading = heading;
		return this;
	}

	public Document withText(final String text) {
		this.text = text;
		return this;
	}

	public Document withDocumentDateTime(final OffsetDateTime documentDateTime) {
		this.documentDateTime = documentDateTime;
		return this;
	}

	public Document withStatus(final String status) {
		this.status = status;
		return this;
	}

	public Document withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Document withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public Document withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Document withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Document withLockedBy(final String lockedBy) {
		this.lockedBy = lockedBy;
		return this;
	}

	public Document withLocked(final OffsetDateTime locked) {
		this.locked = locked;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Document that = (Document) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(source, that.source) && Objects.equals(lifecareId, that.lifecareId)
			&& Objects.equals(type, that.type) && Objects.equals(heading, that.heading)
			&& Objects.equals(text, that.text) && Objects.equals(documentDateTime, that.documentDateTime)
			&& Objects.equals(status, that.status)
			&& Objects.equals(createdBy, that.createdBy) && Objects.equals(created, that.created)
			&& Objects.equals(modifiedBy, that.modifiedBy) && Objects.equals(modified, that.modified)
			&& Objects.equals(lockedBy, that.lockedBy) && Objects.equals(locked, that.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, source, lifecareId, type, heading, text, documentDateTime, status, createdBy, created, modifiedBy, modified, lockedBy, locked);
	}

	@Override
	public String toString() {
		return "Document{id='" + id + "', errandId='" + errandId + "', source='" + source + "', lifecareId='" + lifecareId + "', type='" + type + "', heading='" + heading
			+ "', text='" + text + "', documentDateTime=" + documentDateTime + ", status='"
			+ status + "', createdBy='" + createdBy + "', created=" + created + ", modifiedBy='" + modifiedBy
			+ "', modified=" + modified + ", lockedBy='" + lockedBy + "', locked=" + locked + "}";
	}
}
