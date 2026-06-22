package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

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

	@Schema(description = "Journal entry type (Lifecare 'Typ'/Journaltyp). A municipality-configured value; see the metadata catalogue for a provisional set.", example = "Journalfört meddelande")
	private String type;

	@Schema(description = "Heading (Lifecare 'Rubrik')", example = "Journalfört meddelande: 2025-05-30 Info")
	private String heading;

	@Schema(description = "Free-text body of the journal entry", example = "Hej! Vill bara informera att jag fått jobb på Mejeriet.")
	private String text;

	@Schema(description = "Documented date (Lifecare 'Datum'), distinct from the system created timestamp", example = "2025-05-30")
	private LocalDate entryDate;

	@Schema(description = "Documented time (Lifecare 'Tid'); optional", example = "14:30")
	private LocalTime entryTime;

	@Schema(description = "Skrivskydd status — WORKING is an editable arbetsanteckning, LOCKED is an upprättad handling", allowableValues = {
		"WORKING", "LOCKED"
	}, example = "WORKING")
	private String status;

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'/'Ägare')", example = "carola01winberg")
	private String createdBy;

	@Schema(description = "Created timestamp")
	private OffsetDateTime created;

	@Schema(description = "User id of the last editor (Lifecare 'Ändrat av'); null until the entry has been edited", example = "ebb14eri")
	private String modifiedBy;

	@Schema(description = "Last modified timestamp; null until the entry has been edited")
	private OffsetDateTime modified;

	@Schema(description = "User id of whoever locked the entry; null while WORKING", example = "carola01winberg")
	private String lockedBy;

	@Schema(description = "Timestamp when the entry was locked (became an upprättad handling); null while WORKING")
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

	public LocalDate getEntryDate() {
		return entryDate;
	}

	public LocalTime getEntryTime() {
		return entryTime;
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

	public void setEntryDate(final LocalDate v) {
		this.entryDate = v;
	}

	public void setEntryTime(final LocalTime v) {
		this.entryTime = v;
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

	public JournalEntry withId(final String v) {
		this.id = v;
		return this;
	}

	public JournalEntry withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public JournalEntry withType(final String v) {
		this.type = v;
		return this;
	}

	public JournalEntry withHeading(final String v) {
		this.heading = v;
		return this;
	}

	public JournalEntry withText(final String v) {
		this.text = v;
		return this;
	}

	public JournalEntry withEntryDate(final LocalDate v) {
		this.entryDate = v;
		return this;
	}

	public JournalEntry withEntryTime(final LocalTime v) {
		this.entryTime = v;
		return this;
	}

	public JournalEntry withStatus(final String v) {
		this.status = v;
		return this;
	}

	public JournalEntry withCreatedBy(final String v) {
		this.createdBy = v;
		return this;
	}

	public JournalEntry withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	public JournalEntry withModifiedBy(final String v) {
		this.modifiedBy = v;
		return this;
	}

	public JournalEntry withModified(final OffsetDateTime v) {
		this.modified = v;
		return this;
	}

	public JournalEntry withLockedBy(final String v) {
		this.lockedBy = v;
		return this;
	}

	public JournalEntry withLocked(final OffsetDateTime v) {
		this.locked = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final JournalEntry that = (JournalEntry) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(type, that.type) && Objects.equals(heading, that.heading)
			&& Objects.equals(text, that.text) && Objects.equals(entryDate, that.entryDate)
			&& Objects.equals(entryTime, that.entryTime) && Objects.equals(status, that.status)
			&& Objects.equals(createdBy, that.createdBy) && Objects.equals(created, that.created)
			&& Objects.equals(modifiedBy, that.modifiedBy) && Objects.equals(modified, that.modified)
			&& Objects.equals(lockedBy, that.lockedBy) && Objects.equals(locked, that.locked);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, type, heading, text, entryDate, entryTime, status, createdBy, created, modifiedBy, modified, lockedBy, locked);
	}
}
