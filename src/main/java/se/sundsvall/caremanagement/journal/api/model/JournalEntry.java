package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * A journalanteckning (case-journal entry) attached to an errand — the Lifecare journal shape, captured so it can later
 * be RPA:ed into Lifecare.
 */
@Schema(description = "A journalanteckning (case-journal entry) attached to an errand")
public class JournalEntry {

	@Schema(description = "Unique identifier")
	private String id;

	@Schema(description = "Errand id this journal entry belongs to")
	private String errandId;

	@Schema(description = "Journal entry type (Lifecare 'Typ'/Journaltyp). A municipality-configured value; see the metadata catalogue for a provisional set.", examples = "Journalfört meddelande")
	private String type;

	@Schema(description = "Heading (Lifecare 'Rubrik')", examples = "Journalfört meddelande: 2025-05-30 Info")
	private String heading;

	@Schema(description = "Free-text body of the journal entry", examples = "Hej! Vill bara informera att jag fått jobb på Mejeriet.")
	private String text;

	@Schema(description = "Documented date and time (Lifecare 'Datum'/'Tid'), distinct from the system created timestamp", examples = "2025-05-30T14:30:00+02:00")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime entryDateTime;

	@Schema(description = "Write-protection status — WORKING is an editable working note, LOCKED is a finalised record", allowableValues = {
		"WORKING", "LOCKED"
	}, examples = "WORKING")
	private String status;

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'/'Ägare')", examples = "carola01winberg")
	private String createdBy;

	@Schema(description = "Created timestamp")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "User id of the last editor (Lifecare 'Ändrat av'); null until the entry has been edited", examples = "ebb14eri")
	private String modifiedBy;

	@Schema(description = "Last modified timestamp; null until the entry has been edited")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "User id of whoever locked the entry; null while WORKING", examples = "carola01winberg")
	private String lockedBy;

	@Schema(description = "Timestamp when the entry was locked (became an upprättad handling); null while WORKING")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime locked;

	public static JournalEntry create() {
		return new JournalEntry();
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

	public OffsetDateTime getEntryDateTime() {
		return entryDateTime;
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

	public void setType(final String type) {
		this.type = type;
	}

	public void setHeading(final String heading) {
		this.heading = heading;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public void setEntryDateTime(final OffsetDateTime entryDateTime) {
		this.entryDateTime = entryDateTime;
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

	public JournalEntry withId(final String id) {
		this.id = id;
		return this;
	}

	public JournalEntry withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public JournalEntry withType(final String type) {
		this.type = type;
		return this;
	}

	public JournalEntry withHeading(final String heading) {
		this.heading = heading;
		return this;
	}

	public JournalEntry withText(final String text) {
		this.text = text;
		return this;
	}

	public JournalEntry withEntryDateTime(final OffsetDateTime entryDateTime) {
		this.entryDateTime = entryDateTime;
		return this;
	}

	public JournalEntry withStatus(final String status) {
		this.status = status;
		return this;
	}

	public JournalEntry withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public JournalEntry withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public JournalEntry withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public JournalEntry withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public JournalEntry withLockedBy(final String lockedBy) {
		this.lockedBy = lockedBy;
		return this;
	}

	public JournalEntry withLocked(final OffsetDateTime locked) {
		this.locked = locked;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final JournalEntry that = (JournalEntry) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(type, that.type) && Objects.equals(heading, that.heading)
			&& Objects.equals(text, that.text) && Objects.equals(entryDateTime, that.entryDateTime)
			&& Objects.equals(status, that.status)
			&& Objects.equals(createdBy, that.createdBy) && Objects.equals(created, that.created)
			&& Objects.equals(modifiedBy, that.modifiedBy) && Objects.equals(modified, that.modified)
			&& Objects.equals(lockedBy, that.lockedBy) && Objects.equals(locked, that.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, type, heading, text, entryDateTime, status, createdBy, created, modifiedBy, modified, lockedBy, locked);
	}

	@Override
	public String toString() {
		return "JournalEntry{id='" + id + "', errandId='" + errandId + "', type='" + type + "', heading='" + heading
			+ "', text='" + text + "', entryDateTime=" + entryDateTime + ", status='" + status + "', createdBy='"
			+ createdBy + "', created=" + created + ", modifiedBy='" + modifiedBy + "', modified=" + modified
			+ ", lockedBy='" + lockedBy + "', locked=" + locked + "}";
	}
}
