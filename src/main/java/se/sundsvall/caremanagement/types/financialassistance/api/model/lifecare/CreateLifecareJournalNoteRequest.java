package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "A new journalanteckning, written straight to the errand's insats in Lifecare.")
public class CreateLifecareJournalNoteRequest {

	@Schema(description = "The note body as HTML", examples = "<p>Hej</p>", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 1048576)
	private String content;

	@Schema(description = "Lifecare's noteTypeCode, from the journal-note-types endpoint", examples = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Integer noteTypeCode;

	@Schema(description = "The rubrik; the note type's name when left out", examples = "Telefonsamtal")
	@Size(max = 255)
	private String title;

	@Schema(description = "Documented time, HH:mm; Lifecare stamps the time of saving when left out", examples = "11:50")
	@Pattern(regexp = "^\\d{2}:\\d{2}$", message = "must be HH:mm")
	private String occurenceTime;

	@Schema(description = "Documented date, YYYY-MM-DD; Lifecare's proposal (today) when left out", examples = "2026-09-23")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be YYYY-MM-DD")
	private String occurenceDate;

	@Schema(name = "protected", description = "Saved skrivskyddad, after which it can no longer be changed. The note type's own default when left out.", examples = "false")
	@JsonProperty("protected")
	private Boolean writeProtected;

	public static CreateLifecareJournalNoteRequest create() {
		return new CreateLifecareJournalNoteRequest();
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public CreateLifecareJournalNoteRequest withContent(final String content) {
		this.content = content;
		return this;
	}

	public Integer getNoteTypeCode() {
		return noteTypeCode;
	}

	public void setNoteTypeCode(final Integer noteTypeCode) {
		this.noteTypeCode = noteTypeCode;
	}

	public CreateLifecareJournalNoteRequest withNoteTypeCode(final Integer noteTypeCode) {
		this.noteTypeCode = noteTypeCode;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public CreateLifecareJournalNoteRequest withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getOccurenceTime() {
		return occurenceTime;
	}

	public void setOccurenceTime(final String occurenceTime) {
		this.occurenceTime = occurenceTime;
	}

	public CreateLifecareJournalNoteRequest withOccurenceTime(final String occurenceTime) {
		this.occurenceTime = occurenceTime;
		return this;
	}

	public String getOccurenceDate() {
		return occurenceDate;
	}

	public void setOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
	}

	public CreateLifecareJournalNoteRequest withOccurenceDate(final String occurenceDate) {
		this.occurenceDate = occurenceDate;
		return this;
	}

	public Boolean getWriteProtected() {
		return writeProtected;
	}

	public void setWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
	}

	public CreateLifecareJournalNoteRequest withWriteProtected(final Boolean writeProtected) {
		this.writeProtected = writeProtected;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CreateLifecareJournalNoteRequest that = (CreateLifecareJournalNoteRequest) o;
		return Objects.equals(content, that.content) && Objects.equals(noteTypeCode, that.noteTypeCode) && Objects.equals(title, that.title) && Objects.equals(occurenceTime, that.occurenceTime) && Objects.equals(occurenceDate, that.occurenceDate)
			&& Objects.equals(writeProtected, that.writeProtected);
	}

	@Override
	public int hashCode() {
		return Objects.hash(content, noteTypeCode, title, occurenceTime, occurenceDate, writeProtected);
	}

	@Override
	public String toString() {
		return "CreateLifecareJournalNoteRequest{" + "content='" + content + '\'' + ", noteTypeCode=" + noteTypeCode + ", title='" + title + '\'' + ", occurenceTime='" + occurenceTime + '\'' + ", occurenceDate='" + occurenceDate + '\''
			+ ", writeProtected=" + writeProtected + '}';
	}
}
