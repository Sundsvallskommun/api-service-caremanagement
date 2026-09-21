package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The receipt of a finalize: the recorded decision, whether the process was resumed, the per-task RPA outcome, and the
 * communication channels echoed back so the Draken BFF can send the decision the way the caseworker asked.
 */
@Schema(description = "The receipt of a finalize — decision id, process correlation, RPA tasks and the communication channels to act on.", accessMode = READ_ONLY)
public class FinalizeResponse {

	@Schema(description = "Id of the PAYMENT decision recorded on the errand", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	private String decisionId;

	@Schema(description = "Whether the PaymentDecisionReceived message reached the process. False means the engine could not be reached — the errand stays AWAITING_DECISION and the message must be re-sent via the process-messages endpoint.",
		examples = "true")
	private Boolean processMessageCorrelated;

	@Schema(description = "The RPA write-back tasks the finalize step tried to enqueue, one per Lifecare step")
	private List<RpaTask> rpaTasks;

	@Schema(description = "The communication channels chosen — the frontend sends the decision through these")
	private CommunicationChannels communication;

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

	public List<RpaTask> getRpaTasks() {
		return rpaTasks;
	}

	public void setRpaTasks(final List<RpaTask> rpaTasks) {
		this.rpaTasks = rpaTasks;
	}

	public FinalizeResponse withRpaTasks(final List<RpaTask> rpaTasks) {
		this.rpaTasks = rpaTasks;
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
		return Objects.equals(decisionId, that.decisionId) && Objects.equals(processMessageCorrelated, that.processMessageCorrelated)
			&& Objects.equals(rpaTasks, that.rpaTasks) && Objects.equals(communication, that.communication);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decisionId, processMessageCorrelated, rpaTasks, communication);
	}

	@Override
	public String toString() {
		return "FinalizeResponse{" +
			"decisionId='" + decisionId + '\'' +
			", processMessageCorrelated=" + processMessageCorrelated +
			", rpaTasks=" + rpaTasks +
			", communication=" + communication +
			'}';
	}
}
