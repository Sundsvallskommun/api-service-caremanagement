package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.caremanagement.types.financialassistance.api.validation.ValidFinalizeRequest;

/**
 * The "Besluta och utbetala" request — the decision the caseworker confirmed in Draken and how it is sent. It carries
 * no payments: Draken registers them directly in Lifecare, and the process reads them from there. The cross-field rules
 * (amount required with a granting outcome, period order) are enforced by {@link ValidFinalizeRequest}.
 */
@Schema(description = """
	Finalize a financial assistance errand: record the decision and resume the process. Carries no payments - Draken \
	registers them directly in Lifecare, and the process reads them from there. A payments field from an older client is \
	ignored.""")
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
		return Objects.equals(decision, that.decision) && Objects.equals(communication, that.communication)
			&& Objects.equals(householdSizeChanged, that.householdSizeChanged);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decision, communication, householdSizeChanged);
	}

	@Override
	public String toString() {
		return "FinalizeRequest{" +
			"decision=" + decision +
			", communication=" + communication +
			", householdSizeChanged=" + householdSizeChanged +
			'}';
	}
}
