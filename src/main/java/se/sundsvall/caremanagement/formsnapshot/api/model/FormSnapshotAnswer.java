package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * The answer the applicant gave to a field, captured as it was presented. {@code display} is always the human-readable
 * string the applicant saw (and a renderer prints); {@code code} (for ENUM/option answers) and {@code value} (for
 * free-text/number/boolean answers) are retained for machine reconciliation against the typed application data. A
 * boolean answer carries both, e.g. {@code value="true"}, {@code display="Ja"}.
 */
@Schema(description = "The answer given to a field, as it was presented to the applicant.")
public class FormSnapshotAnswer {

	@Schema(description = "The option code, when the answer is an enum/option value; null otherwise", examples = "SINGLE")
	private String code;

	@Schema(description = "The raw value, when the answer is free text / number / boolean; null otherwise", examples = "12000")
	private String value;

	@Schema(description = "The human-readable answer text the applicant saw", examples = "Ensamstående")
	private String display;

	public static FormSnapshotAnswer create() {
		return new FormSnapshotAnswer();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public FormSnapshotAnswer withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public FormSnapshotAnswer withValue(final String value) {
		this.value = value;
		return this;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(final String display) {
		this.display = display;
	}

	public FormSnapshotAnswer withDisplay(final String display) {
		this.display = display;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotAnswer that = (FormSnapshotAnswer) o;
		return Objects.equals(code, that.code) && Objects.equals(value, that.value) && Objects.equals(display, that.display);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, value, display);
	}

	@Override
	public String toString() {
		return "FormSnapshotAnswer{code='" + code + "', value='" + value + "', display='" + display + "'}";
	}
}
