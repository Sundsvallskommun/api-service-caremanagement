package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single option (radio button / checkbox / select entry) exactly as it was presented to the applicant, in render
 * order. Captures both the machine {@code code} and the human {@code label} the applicant saw — closing the gap that
 * the backend schema holds only codes while the labels live in the frontend.
 */
@Schema(description = "An option as presented to the applicant.")
public class FormSnapshotOption {

	@Schema(description = "The option code (machine value)", examples = "SINGLE")
	private String code;

	@Schema(description = "The option label the applicant saw", examples = "Ensamstående")
	private String label;

	@Schema(description = "Whether the applicant selected this option", examples = "true")
	private boolean selected;

	public static FormSnapshotOption create() {
		return new FormSnapshotOption();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public FormSnapshotOption withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public FormSnapshotOption withLabel(final String label) {
		this.label = label;
		return this;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(final boolean selected) {
		this.selected = selected;
	}

	public FormSnapshotOption withSelected(final boolean selected) {
		this.selected = selected;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotOption that = (FormSnapshotOption) o;
		return selected == that.selected && Objects.equals(code, that.code) && Objects.equals(label, that.label);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, label, selected);
	}

	@Override
	public String toString() {
		return "FormSnapshotOption{code='" + code + "', label='" + label + "', selected=" + selected + "}";
	}
}
