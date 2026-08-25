package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Request to set the approval state of one financial assistance view section. {@code approved=true} records the section
 * as verified by the logged-in caseworker — taken from the {@code X-Sent-By} identity of the caller, not the request
 * body, so the approver can't be spoofed. {@code approved=false} withdraws an earlier approval. The approver is stored
 * only when approving — withdrawing clears who/when.
 */
@Schema(description = "Set the approval state of a financial assistance view section.")
public class SectionApprovalRequest {

	@Schema(description = "Whether the section is approved (true) or its approval withdrawn (false)", examples = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Boolean approved;

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

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SectionApprovalRequest that = (SectionApprovalRequest) o;
		return Objects.equals(approved, that.approved);
	}

	@Override
	public int hashCode() {
		return Objects.hash(approved);
	}

	@Override
	public String toString() {
		return "SectionApprovalRequest{approved=" + approved + '}';
	}
}
