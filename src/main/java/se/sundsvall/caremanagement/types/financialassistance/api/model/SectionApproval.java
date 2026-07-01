package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * One section of the Draken financial assistance view as an acknowledgeable object — whether a caseworker has verified
 * it as approved.
 * The section is {@code CALCULATION} (calculation), {@code PAYMENT} (payment) or {@code DECISION} (decision).
 * {@code approvedBy} / {@code approvedAt} are populated while approved and null once the approval is withdrawn.
 */
@Schema(description = "A caseworker's approval of one section of the financial assistance view (calculation / payment / decision).")
public class SectionApproval {

	@Schema(description = "The section this approval concerns", examples = "CALCULATION", allowableValues = {
		"CALCULATION", "PAYMENT", "DECISION"
	})
	private String section;

	@Schema(description = "Whether the section has been verified as approved by a caseworker", examples = "true")
	private boolean approved;

	@Schema(description = "The caseworker who approved the section (null while not approved)", examples = "jane02doe", accessMode = READ_ONLY)
	private String approvedBy;

	@Schema(description = "When the section was approved (null while not approved)", accessMode = READ_ONLY)
	private OffsetDateTime approvedAt;

	public static SectionApproval create() {
		return new SectionApproval();
	}

	public String getSection() {
		return section;
	}

	public void setSection(final String section) {
		this.section = section;
	}

	public SectionApproval withSection(final String section) {
		this.section = section;
		return this;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(final boolean approved) {
		this.approved = approved;
	}

	public SectionApproval withApproved(final boolean approved) {
		this.approved = approved;
		return this;
	}

	public String getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(final String approvedBy) {
		this.approvedBy = approvedBy;
	}

	public SectionApproval withApprovedBy(final String approvedBy) {
		this.approvedBy = approvedBy;
		return this;
	}

	public OffsetDateTime getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(final OffsetDateTime approvedAt) {
		this.approvedAt = approvedAt;
	}

	public SectionApproval withApprovedAt(final OffsetDateTime approvedAt) {
		this.approvedAt = approvedAt;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SectionApproval that = (SectionApproval) o;
		return approved == that.approved && Objects.equals(section, that.section)
			&& Objects.equals(approvedBy, that.approvedBy) && Objects.equals(approvedAt, that.approvedAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(section, approved, approvedBy, approvedAt);
	}

	@Override
	public String toString() {
		return "SectionApproval{" +
			"section='" + section + '\'' +
			", approved=" + approved +
			", approvedBy='" + approvedBy + '\'' +
			", approvedAt=" + approvedAt +
			'}';
	}
}
