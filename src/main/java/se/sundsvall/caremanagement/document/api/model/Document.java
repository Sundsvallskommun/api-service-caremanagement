package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A Dokument (formal case document) attached to an errand — the Lifecare document shape, captured so it can later be
 * RPA:ed into Lifecare.
 */
@Schema(description = "A Dokument (formal case document) attached to an errand")
public class Document {

	@Schema(description = "Unique identifier")
	private String id;

	@Schema(description = "Errand id this document belongs to")
	private String errandId;

	@Schema(description = "Document type (Lifecare 'Typ'/Dokumenttyp). A municipality-configured value; see the metadata catalogue for a provisional set.", example = "Brev")
	private String type;

	@Schema(description = "Heading (Lifecare 'Rubrik')", example = "Beslut om ekonomiskt bistånd 2025-05")
	private String heading;

	@Schema(description = "Free-text body of the document", example = "Beslut har fattats enligt nedan ...")
	private String text;

	@Schema(description = "Documented date (Lifecare 'Datum'), distinct from the system created timestamp", example = "2025-05-30")
	private LocalDate documentDate;

	@Schema(description = "Documented time (Lifecare 'Tid'); optional", example = "14:30")
	private LocalTime documentTime;

	@Schema(description = "Skrivskydd status — WORKING is an editable draft, LOCKED is an upprättad handling", allowableValues = {
		"WORKING", "LOCKED"
	}, example = "WORKING")
	private String status;

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'/'Ägare')", example = "carola01winberg")
	private String createdBy;

	@Schema(description = "Created timestamp")
	private OffsetDateTime created;

	@Schema(description = "User id of the last editor (Lifecare 'Ändrat av'); null until the document has been edited", example = "ebb14eri")
	private String modifiedBy;

	@Schema(description = "Last modified timestamp; null until the document has been edited")
	private OffsetDateTime modified;

	@Schema(description = "User id of whoever locked the document; null while WORKING", example = "carola01winberg")
	private String lockedBy;

	@Schema(description = "Timestamp when the document was locked (became an upprättad handling); null while WORKING")
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

	public String getType() {
		return type;
	}

	public String getHeading() {
		return heading;
	}

	public String getText() {
		return text;
	}

	public LocalDate getDocumentDate() {
		return documentDate;
	}

	public LocalTime getDocumentTime() {
		return documentTime;
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

	public void setId(final String v) {
		this.id = v;
	}

	public void setErrandId(final String v) {
		this.errandId = v;
	}

	public void setType(final String v) {
		this.type = v;
	}

	public void setHeading(final String v) {
		this.heading = v;
	}

	public void setText(final String v) {
		this.text = v;
	}

	public void setDocumentDate(final LocalDate v) {
		this.documentDate = v;
	}

	public void setDocumentTime(final LocalTime v) {
		this.documentTime = v;
	}

	public void setStatus(final String v) {
		this.status = v;
	}

	public void setCreatedBy(final String v) {
		this.createdBy = v;
	}

	public void setCreated(final OffsetDateTime v) {
		this.created = v;
	}

	public void setModifiedBy(final String v) {
		this.modifiedBy = v;
	}

	public void setModified(final OffsetDateTime v) {
		this.modified = v;
	}

	public void setLockedBy(final String v) {
		this.lockedBy = v;
	}

	public void setLocked(final OffsetDateTime v) {
		this.locked = v;
	}

	public Document withId(final String v) {
		this.id = v;
		return this;
	}

	public Document withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public Document withType(final String v) {
		this.type = v;
		return this;
	}

	public Document withHeading(final String v) {
		this.heading = v;
		return this;
	}

	public Document withText(final String v) {
		this.text = v;
		return this;
	}

	public Document withDocumentDate(final LocalDate v) {
		this.documentDate = v;
		return this;
	}

	public Document withDocumentTime(final LocalTime v) {
		this.documentTime = v;
		return this;
	}

	public Document withStatus(final String v) {
		this.status = v;
		return this;
	}

	public Document withCreatedBy(final String v) {
		this.createdBy = v;
		return this;
	}

	public Document withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	public Document withModifiedBy(final String v) {
		this.modifiedBy = v;
		return this;
	}

	public Document withModified(final OffsetDateTime v) {
		this.modified = v;
		return this;
	}

	public Document withLockedBy(final String v) {
		this.lockedBy = v;
		return this;
	}

	public Document withLocked(final OffsetDateTime v) {
		this.locked = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Document that = (Document) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(type, that.type) && Objects.equals(heading, that.heading)
			&& Objects.equals(text, that.text) && Objects.equals(documentDate, that.documentDate)
			&& Objects.equals(documentTime, that.documentTime) && Objects.equals(status, that.status)
			&& Objects.equals(createdBy, that.createdBy) && Objects.equals(created, that.created)
			&& Objects.equals(modifiedBy, that.modifiedBy) && Objects.equals(modified, that.modified)
			&& Objects.equals(lockedBy, that.lockedBy) && Objects.equals(locked, that.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, type, heading, text, documentDate, documentTime, status, createdBy, created, modifiedBy, modified, lockedBy, locked);
	}
}
