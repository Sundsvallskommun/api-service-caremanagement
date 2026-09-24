package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.types.financialassistance.api.validation.ValidFinalizeRequest;

/**
 * The "Besluta och utbetala" request — everything the caseworker confirmed in Draken once the calculation, payment and
 * decision sections were approved. The cross-field rules (payments iff the outcome carries an amount, amount required
 * with a granting outcome, period order) are enforced by {@link ValidFinalizeRequest}.
 */
@Schema(description = "Finalize a financial assistance errand: record the decision and its payments and resume the process.")
@ValidFinalizeRequest
public class FinalizeRequest {

	@Schema(description = "The decision", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Valid
	private FinalizeDecision decision;

	@Schema(description = "The channels chosen for sending the calculation and decision to the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Valid
	private CommunicationChannels communication;

	@Schema(description = "The payments to register in Lifecare. Required (at least one) when the outcome carries an amount; must be empty for AVSLAG.")
	@Valid
	private List<FinalizePayment> payments;

	@Schema(
		description = "Whether the caseworker changed the household size (gemensamma kostnader) in the calculation draft. Recorded on the errand and served on the view. Defaults to false.",
		examples = "false")
	private Boolean householdSizeChanged;

	public static FinalizeRequest create() {
		return new FinalizeRequest();
	}

	public FinalizeDecision getDecision() {
		return decision;
	}

	public void setDecision(final FinalizeDecision decision) {
		this.decision = decision;
	}

	public FinalizeRequest withDecision(final FinalizeDecision decision) {
		this.decision = decision;
		return this;
	}

	public CommunicationChannels getCommunication() {
		return communication;
	}

	public void setCommunication(final CommunicationChannels communication) {
		this.communication = communication;
	}

	public FinalizeRequest withCommunication(final CommunicationChannels communication) {
		this.communication = communication;
		return this;
	}

	public List<FinalizePayment> getPayments() {
		return payments;
	}

	public void setPayments(final List<FinalizePayment> payments) {
		this.payments = payments;
	}

	public FinalizeRequest withPayments(final List<FinalizePayment> payments) {
		this.payments = payments;
		return this;
	}

	public Boolean getHouseholdSizeChanged() {
		return householdSizeChanged;
	}

	public void setHouseholdSizeChanged(final Boolean householdSizeChanged) {
		this.householdSizeChanged = householdSizeChanged;
	}

	public FinalizeRequest withHouseholdSizeChanged(final Boolean householdSizeChanged) {
		this.householdSizeChanged = householdSizeChanged;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinalizeRequest that = (FinalizeRequest) o;
		return Objects.equals(decision, that.decision) && Objects.equals(communication, that.communication) && Objects.equals(payments, that.payments)
			&& Objects.equals(householdSizeChanged, that.householdSizeChanged);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decision, communication, payments, householdSizeChanged);
	}

	@Override
	public String toString() {
		return "FinalizeRequest{" +
			"decision=" + decision +
			", communication=" + communication +
			", payments=" + payments +
			", householdSizeChanged=" + householdSizeChanged +
			'}';
	}
}
