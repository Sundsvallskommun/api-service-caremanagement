package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The receipt of a finalize: the recorded decision and payments, whether the process was resumed, payee warnings, and
 * the communication channels echoed back so the Draken BFF can send the decision the way the caseworker asked.
 */
@Schema(description = "The receipt of a finalize — decision id, payment ids, process correlation, payee warnings and the communication channels to act on.", accessMode = READ_ONLY)
public class FinalizeResponse {

	@Schema(description = "Id of the PAYMENT decision recorded on the errand", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String decisionId;

	@ArraySchema(arraySchema = @Schema(
		description = "The ids of the Payment rows the finalize created, in request order. Draken's BFF reads each through GET .../payments/{paymentId}, registers it in Lifecare and reports back through .../payments/{paymentId}/lifecare-result."),
		schema = @Schema(implementation = String.class, examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479"))
	private List<String> paymentIds;

	@Schema(
		description = "Whether the PaymentDecisionReceived message reached the process. False means it did not reach it now — the errand stays AWAITING_DECISION and the message is queued and re-sent automatically (at most an hour apart, for three days). Draken should say so; correlating by hand through the process-messages endpoint is only needed if the retry gives up.",
		examples = "true")
	private Boolean processMessageCorrelated;

	@Schema(description = "The communication channels chosen — the frontend sends the decision through these")
	private CommunicationChannels communication;

	@ArraySchema(arraySchema = @Schema(
		description = "Warnings about the payees the decision pays to — a payment cannot be registered in Lifecare against a payee that is not there yet. Present when a payment names a manually added payee careM has not seen reported SYNCED (payees/{payeeId}/lifecare-result). Empty when every payee is in Lifecare. The finalize itself is not blocked by these; the decision and the payment rows are created either way."),
		schema = @Schema(implementation = String.class, examples = "Betalningsmottagaren \"Sundsvalls Hyresbostäder AB\" är inte upplagd i Lifecare ännu – utbetalningen kan inte registreras förrän den har lagts upp där."))
	private List<String> payeeWarnings;

	public static FinalizeResponse create() {
		return new FinalizeResponse();
	}

	public List<String> getPayeeWarnings() {
		return payeeWarnings;
	}

	public void setPayeeWarnings(final List<String> payeeWarnings) {
		this.payeeWarnings = payeeWarnings;
	}

	public FinalizeResponse withPayeeWarnings(final List<String> payeeWarnings) {
		this.payeeWarnings = payeeWarnings;
		return this;
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

	public List<String> getPaymentIds() {
		return paymentIds;
	}

	public void setPaymentIds(final List<String> paymentIds) {
		this.paymentIds = paymentIds;
	}

	public FinalizeResponse withPaymentIds(final List<String> paymentIds) {
		this.paymentIds = paymentIds;
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

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinalizeResponse that = (FinalizeResponse) o;
		return Objects.equals(decisionId, that.decisionId) && Objects.equals(paymentIds, that.paymentIds) && Objects.equals(processMessageCorrelated, that.processMessageCorrelated)
			&& Objects.equals(communication, that.communication) && Objects.equals(payeeWarnings, that.payeeWarnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decisionId, paymentIds, processMessageCorrelated, communication, payeeWarnings);
	}

	@Override
	public String toString() {
		return "FinalizeResponse{" +
			"decisionId='" + decisionId + '\'' +
			", paymentIds=" + paymentIds +
			", processMessageCorrelated=" + processMessageCorrelated +
			", communication=" + communication +
			", payeeWarnings=" + payeeWarnings +
			'}';
	}
}
