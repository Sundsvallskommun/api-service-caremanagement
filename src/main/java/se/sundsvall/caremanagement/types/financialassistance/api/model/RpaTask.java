package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The receipt for one RPA write-back the finalize step tried to enqueue. {@code enqueued=false} means the item is not
 * on the UiPath queue — either the integration is disabled or the Orchestrator was unreachable — and the Lifecare step
 * has to be done by hand or re-enqueued via the RPA endpoint.
 */
@Schema(description = "One RPA write-back task the finalize step tried to enqueue.", accessMode = READ_ONLY)
public class RpaTask {

	@Schema(description = "The RPA action", examples = "WRITE_DECISION")
	private String action;

	@Schema(description = "The queue item reference the Orchestrator knows the task by", examples = "FINANCIAL_ASSISTANCE:cb20c51f-fcf3-42c0-b613-de563634a8ec:WRITE_DECISION")
	private String reference;

	@Schema(description = "Whether the task is on the queue (false when RPA is disabled or the enqueue failed)", examples = "true")
	private Boolean enqueued;

	public static RpaTask create() {
		return new RpaTask();
	}

	public String getAction() {
		return action;
	}

	public void setAction(final String action) {
		this.action = action;
	}

	public RpaTask withAction(final String action) {
		this.action = action;
		return this;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(final String reference) {
		this.reference = reference;
	}

	public RpaTask withReference(final String reference) {
		this.reference = reference;
		return this;
	}

	public Boolean getEnqueued() {
		return enqueued;
	}

	public void setEnqueued(final Boolean enqueued) {
		this.enqueued = enqueued;
	}

	public RpaTask withEnqueued(final Boolean enqueued) {
		this.enqueued = enqueued;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final RpaTask that = (RpaTask) o;
		return Objects.equals(action, that.action) && Objects.equals(reference, that.reference) && Objects.equals(enqueued, that.enqueued);
	}

	@Override
	public int hashCode() {
		return Objects.hash(action, reference, enqueued);
	}

	@Override
	public String toString() {
		return "RpaTask{" +
			"action='" + action + '\'' +
			", reference='" + reference + '\'' +
			", enqueued=" + enqueued +
			'}';
	}
}
