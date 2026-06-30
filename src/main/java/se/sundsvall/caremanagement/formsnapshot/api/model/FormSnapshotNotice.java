package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * An info / warning / error box that was actually rendered next to a field or section when the applicant saw it.
 * Captures the legally relevant "what was the applicant told here" content.
 */
@Schema(description = "An info / warning / error notice shown to the applicant.")
public class FormSnapshotNotice {

	@Schema(description = "The notice level", examples = "WARNING", allowableValues = {
		"INFO", "WARNING", "ERROR"
	})
	private String level;

	@Schema(description = "The notice text the applicant saw", examples = "Felaktiga uppgifter kan vara bidragsbrott.")
	private String text;

	public static FormSnapshotNotice create() {
		return new FormSnapshotNotice();
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(final String level) {
		this.level = level;
	}

	public FormSnapshotNotice withLevel(final String level) {
		this.level = level;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public FormSnapshotNotice withText(final String text) {
		this.text = text;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotNotice that = (FormSnapshotNotice) o;
		return Objects.equals(level, that.level) && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(level, text);
	}

	@Override
	public String toString() {
		return "FormSnapshotNotice{level='" + level + "', text='" + text + "'}";
	}
}
