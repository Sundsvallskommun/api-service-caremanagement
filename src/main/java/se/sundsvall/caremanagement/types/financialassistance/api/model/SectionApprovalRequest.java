package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Request to set the approval state of one EB view section. {@code approved=true} records the section as verified by
 * {@code approvedBy} (the logged-in handläggare); {@code approved=false} withdraws an earlier approval. The approver is
 * stored only when approving — withdrawing clears who/when.
 */
@Schema(description = "Set the approval state of an EB view section.")
public class SectionApprovalRequest {

	@Schema(description = "Whether the section is approved (true) or its approval withdrawn (false)", examples = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Boolean approved;

	@Schema(description = "The handläggare approving the section (stored when approving, ignored when withdrawing)", examples = "jane02doe")
	private String approvedBy;

	public static SectionApprovalRequest create() {
		return new SectionApprovalRequest();
	}

	public Boolean getApproved() {
		return approved;
	}

	public void setApproved(final Boolean approved) {
		this.approved = approved;
	}

	public SectionApprovalRequest withApproved(final Boolean approved) {
		this.approved = approved;
		return this;
	}

	public String getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(final String approvedBy) {
		this.approvedBy = approvedBy;
	}

	public SectionApprovalRequest withApprovedBy(final String approvedBy) {
		this.approvedBy = approvedBy;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SectionApprovalRequest that = (SectionApprovalRequest) o;
		return Objects.equals(approved, that.approved) && Objects.equals(approvedBy, that.approvedBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(approved, approvedBy);
	}

	@Override
	public String toString() {
		return "SectionApprovalRequest{" +
			"approved=" + approved +
			", approvedBy='" + approvedBy + '\'' +
			'}';
	}
}
