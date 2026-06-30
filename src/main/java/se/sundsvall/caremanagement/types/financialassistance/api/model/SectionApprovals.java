package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * The three approvable sections of the Draken EB view bundled into one object, so the frontend can render — and the
 * caseworker can acknowledge — each tab's approval state in a single place. Always carries all three (a section never
 * approved is present with {@code approved=false}).
 */
@Schema(description = "The caseworker approval state of the three EB view sections (calculation, payment, decision).")
public class SectionApprovals {

	@Schema(description = "Approval of the calculation (calculation) section")
	private SectionApproval calculation;

	@Schema(description = "Approval of the payment (payment) section")
	private SectionApproval payment;

	@Schema(description = "Approval of the decision (decision) section")
	private SectionApproval decision;

	public static SectionApprovals create() {
		return new SectionApprovals();
	}

	public SectionApproval getCalculation() {
		return calculation;
	}

	public void setCalculation(final SectionApproval calculation) {
		this.calculation = calculation;
	}

	public SectionApprovals withCalculation(final SectionApproval calculation) {
		this.calculation = calculation;
		return this;
	}

	public SectionApproval getPayment() {
		return payment;
	}

	public void setPayment(final SectionApproval payment) {
		this.payment = payment;
	}

	public SectionApprovals withPayment(final SectionApproval payment) {
		this.payment = payment;
		return this;
	}

	public SectionApproval getDecision() {
		return decision;
	}

	public void setDecision(final SectionApproval decision) {
		this.decision = decision;
	}

	public SectionApprovals withDecision(final SectionApproval decision) {
		this.decision = decision;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final SectionApprovals that = (SectionApprovals) o;
		return Objects.equals(calculation, that.calculation) && Objects.equals(payment, that.payment)
			&& Objects.equals(decision, that.decision);
	}

	@Override
	public int hashCode() {
		return Objects.hash(calculation, payment, decision);
	}

	@Override
	public String toString() {
		return "SectionApprovals{" +
			"calculation=" + calculation +
			", payment=" + payment +
			", decision=" + decision +
			'}';
	}
}
