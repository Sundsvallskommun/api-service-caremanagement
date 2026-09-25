package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A Lifecare journalanteckning or document row, cleaned up for the UI.")
public class LifecareRecord {

	@Schema(description = "The Lifecare record id", examples = "138")
	private String id;

	@Schema(description = "Which group the record is shown under", examples = "JOURNAL_NOTE", allowableValues = {
		"JOURNAL_NOTE", "DOCUMENT"
	})
	private String category;

	@Schema(description = "The rubrik", examples = "Journalanteckning")
	private String title;

	@Schema(description = "Documented date and time, date and time joined (YYYY-MM-DDTHH:mm, or just the date)", examples = "2026-09-23T12:11")
	private String dateTime;

	@Schema(description = "Lifecare's type label", examples = "Journalanteckning")
	private String type;

	@Schema(description = "The akt or utredning the row sits under", examples = "EK Ekonomiskt bistånd")
	private String ownerTypeText;

	@Schema(description = "The responsible caseworker, when Lifecare names one", examples = "RPA_031DEV")
	private String responsibleCaseworker;

	@Schema(description = "Who last changed it and when, as Lifecare presents it", examples = "RPA_031DEV 2026-09-23")
	private String modifiedBy;

	@Schema(description = "Whether Lifecare has locked the record", examples = "false")
	private Boolean locked;

	@Schema(name = "protected", description = "Whether the record is skrivskyddad (finalised) in Lifecare", examples = "false")
	@JsonProperty("protected")
	private Boolean writeProtected;

	@Schema(description = "What kind of record Lifecare says it is: Regular (a written document), Form (a blankett), Pdf (a stored file, e.g. an inkommen handling) or JournalNote", examples = "Pdf")
	private String documentKind;

	public static LifecareRecord create() {
		return new LifecareRecord();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public LifecareRecord withId(final String id) {
		this.id = id;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public LifecareRecord withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public LifecareRecord withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDateTime() {
		return dateTime;
	}

	public void setDateTime(final String dateTime) {
		this.dateTime = dateTime;
	}

	public LifecareRecord withDateTime(final String dateTime) {
		this.dateTime = dateTime;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public LifecareRecord withType(final String type) {
		this.type = type;
		return this;
	}

	public String getOwnerTypeText() {
		return ownerTypeText;
	}

	public void setOwnerTypeText(final String ownerTypeText) {
		this.ownerTypeText = ownerTypeText;
	}

	public LifecareRecord withOwnerTypeText(final String ownerTypeText) {
		this.ownerTypeText = ownerTypeText;
		return this;
	}

	public String getResponsibleCaseworker() {
		return responsibleCaseworker;
	}

	public void setResponsibleCaseworker(final String responsibleCaseworker) {
		this.responsibleCaseworker = responsibleCaseworker;
	}

	public LifecareRecord withResponsibleCaseworker(final String responsibleCaseworker) {
		this.responsibleCaseworker = responsibleCaseworker;
		return this;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public LifecareRecord withModifiedBy(final String modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Boolean getLocked() {
		return locked;
	}

	public void setLocked(final Boolean locked) {
		this.locked = locked;
	}

	public LifecareRecord withLocked(final Boolean locked) {
		this.locked = locked;
		return this;
	}

	public Boolean getWriteProtected() {
		return writeProtected;
	}

	public void setWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
	}

	public LifecareRecord withWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
		return this;
	}

	public String getDocumentKind() {
		return documentKind;
	}

	public void setDocumentKind(final String documentKind) {
		this.documentKind = documentKind;
	}

	public LifecareRecord withDocumentKind(final String documentKind) {
		this.documentKind = documentKind;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareRecord that = (LifecareRecord) o;
		return Objects.equals(id, that.id) && Objects.equals(category, that.category) && Objects.equals(title, that.title) && Objects.equals(dateTime, that.dateTime) && Objects.equals(type, that.type) && Objects.equals(ownerTypeText, that.ownerTypeText)
			&& Objects.equals(responsibleCaseworker, that.responsibleCaseworker) && Objects.equals(modifiedBy, that.modifiedBy) && Objects.equals(locked, that.locked) && Objects.equals(writeProtected, that.writeProtected)
			&& Objects.equals(documentKind, that.documentKind);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, category, title, dateTime, type, ownerTypeText, responsibleCaseworker, modifiedBy, locked, writeProtected, documentKind);
	}

	@Override
	public String toString() {
		return "LifecareRecord{" + "id='" + id + '\'' + ", category='" + category + '\'' + ", title='" + title + '\'' + ", dateTime='" + dateTime + '\'' + ", type='" + type + '\'' + ", ownerTypeText='" + ownerTypeText + '\'' + ", responsibleCaseworker='"
			+ responsibleCaseworker + '\'' + ", modifiedBy='" + modifiedBy + '\'' + ", locked=" + locked + ", writeProtected=" + writeProtected + ", documentKind='" + documentKind + '\'' + '}';
	}
}
