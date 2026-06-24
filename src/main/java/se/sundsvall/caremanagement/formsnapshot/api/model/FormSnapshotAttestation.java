package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * The attestation (intygande) the applicant accepted at submission — the exact declaration text shown and the answer
 * given. Captured separately from the sections because it is the legally load-bearing consent.
 */
@Schema(description = "The attestation the applicant accepted at submission.")
public class FormSnapshotAttestation {

	@Schema(description = "The attestation text the applicant accepted", examples = "Jag intygar på heder och samvete att uppgifterna är riktiga.")
	private String label;

	@Schema(description = "The answer given to the attestation")
	private FormSnapshotAnswer answer;

	public static FormSnapshotAttestation create() {
		return new FormSnapshotAttestation();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public FormSnapshotAttestation withLabel(final String label) {
		this.label = label;
		return this;
	}

	public FormSnapshotAnswer getAnswer() {
		return answer;
	}

	public void setAnswer(final FormSnapshotAnswer answer) {
		this.answer = answer;
	}

	public FormSnapshotAttestation withAnswer(final FormSnapshotAnswer answer) {
		this.answer = answer;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotAttestation that = (FormSnapshotAttestation) o;
		return Objects.equals(label, that.label) && Objects.equals(answer, that.answer);
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, answer);
	}

	@Override
	public String toString() {
		return "FormSnapshotAttestation{label='" + label + "', answer=" + answer + "}";
	}
}
