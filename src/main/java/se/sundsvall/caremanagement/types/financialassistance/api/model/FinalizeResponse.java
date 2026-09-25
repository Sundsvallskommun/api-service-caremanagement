package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionRegistration;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The receipt of a finalize: the recorded decision, whether the process was resumed, and the communication channels
 * echoed back so the Draken BFF can send the decision the way the caseworker asked. No payment ids: careM creates no
 * payments — the ones the decision pays with are already in Lifecare and linked to the errand.
 */
@Schema(description = "The receipt of a finalize — decision id, process correlation and the communication channels to act on.", accessMode = READ_ONLY)
public class FinalizeResponse {

	@Schema(description = "Id of the PAYMENT decision recorded on the errand", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String decisionId;

	@Schema(
		description = "Whether the PaymentDecisionReceived message reached the process. False means it did not reach it now — the errand stays AWAITING_DECISION and the message is queued and re-sent automatically (at most an hour apart, for three days). Draken should say so; correlating by hand through the process-messages endpoint is only needed if the retry gives up.",
		examples = "true")
	private Boolean processMessageCorrelated;

	@Schema(description = "The communication channels chosen — the frontend sends the decision through these")
	private CommunicationChannels communication;

	@Schema(description = """
		How the recorded decision was tied to the errand's beslut in Lifecare (lifecareDecisionId). careM receipts it itself, \
		so a client no longer has to post .../decisions/{decisionId}/lifecare-result after finalizing.""")
	private LifecareDecisionRegistration lifecareDecision;

	public static FinalizeResponse create() {
		return new FinalizeResponse();
	}

	public String getDecisionId() {
		return decisionId;
	}

	public void setDecisionId(final String decisionId) {
		this.decisionId = decisionId;
	}

	public FinalizeResponse withDecisionId(final String decisionId) {
		this.decisionId = decisionId;
		return this;
	}

	public Boolean getProcessMessageCorrelated() {
		return processMessageCorrelated;
	}

	public void setProcessMessageCorrelated(final Boolean processMessageCorrelated) {
		this.processMessageCorrelated = processMessageCorrelated;
	}

	public FinalizeResponse withProcessMessageCorrelated(final Boolean processMessageCorrelated) {
		this.processMessageCorrelated = processMessageCorrelated;
		return this;
	}

	public CommunicationChannels getCommunication() {
		return communication;
	}

	public void setCommunication(final CommunicationChannels communication) {
		this.communication = communication;
	}

	public FinalizeResponse withCommunication(final CommunicationChannels communication) {
		this.communication = communication;
		return this;
	}

	public LifecareDecisionRegistration getLifecareDecision() {
		return lifecareDecision;
	}

	public void setLifecareDecision(final LifecareDecisionRegistration lifecareDecision) {
		this.lifecareDecision = lifecareDecision;
	}

	public FinalizeResponse withLifecareDecision(final LifecareDecisionRegistration lifecareDecision) {
		this.lifecareDecision = lifecareDecision;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinalizeResponse that = (FinalizeResponse) o;
		return Objects.equals(decisionId, that.decisionId) && Objects.equals(processMessageCorrelated, that.processMessageCorrelated)
			&& Objects.equals(communication, that.communication) && Objects.equals(lifecareDecision, that.lifecareDecision);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decisionId, processMessageCorrelated, communication, lifecareDecision);
	}

	@Override
	public String toString() {
		return "FinalizeResponse{" +
			"decisionId='" + decisionId + '\'' +
			", processMessageCorrelated=" + processMessageCorrelated +
			", communication=" + communication +
			", lifecareDecision=" + lifecareDecision +
			'}';
	}
}
