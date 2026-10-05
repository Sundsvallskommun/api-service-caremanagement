package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Result of reading the Lifecare payment status for an application month: whether it has been effectuated, when it
 * has the payment date, and when it has not, why.
 */
@Schema(description = "Whether the Lifecare payment for the application month has been effectuated.")
public class PaymentStatusResponse {

	@Schema(description = "True when Lifecare reports the errand's payments paid (or, without an errand, a payment for the application month)", examples = "true")
	private Boolean effectuated;

	@Schema(description = "The date the payment was made (Lifecare PayDate), when effectuated", examples = "2026-05-27")
	private String paymentDate;

	@Schema(description = "Why the status is not effectuated, in words a caseworker can act on; empty when effectuated", examples = "1 av 2 kopplade utbetalningar är inte utbetalda i Lifecare ännu")
	private String detail;

	@Schema(description = "The last working day the errand's payments may wait, counted from the decision (ISO date); absent without an errand", examples = "2026-09-28")
	private String deadline;

	@Schema(description = "True when the payments are still not effectuated after the deadline — the process then notifies the caseworker. Never closes anything.", examples = "false")
	private Boolean overdue;

	public static PaymentStatusResponse create() {
		return new PaymentStatusResponse();
	}

	public Boolean getEffectuated() {
		return effectuated;
	}

	public void setEffectuated(final Boolean effectuated) {
		this.effectuated = effectuated;
	}

	public PaymentStatusResponse withEffectuated(final Boolean effectuated) {
		this.effectuated = effectuated;
		return this;
	}

	public String getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(final String paymentDate) {
		this.paymentDate = paymentDate;
	}

	public PaymentStatusResponse withPaymentDate(final String paymentDate) {
		this.paymentDate = paymentDate;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(final String detail) {
		this.detail = detail;
	}

	public PaymentStatusResponse withDetail(final String detail) {
		this.detail = detail;
		return this;
	}

	public String getDeadline() {
		return deadline;
	}

	public void setDeadline(final String deadline) {
		this.deadline = deadline;
	}

	public PaymentStatusResponse withDeadline(final String deadline) {
		this.deadline = deadline;
		return this;
	}

	public Boolean getOverdue() {
		return overdue;
	}

	public void setOverdue(final Boolean overdue) {
		this.overdue = overdue;
	}

	public PaymentStatusResponse withOverdue(final Boolean overdue) {
		this.overdue = overdue;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PaymentStatusResponse that = (PaymentStatusResponse) o;
		return Objects.equals(effectuated, that.effectuated) && Objects.equals(paymentDate, that.paymentDate) && Objects.equals(detail, that.detail) && Objects.equals(deadline, that.deadline)
			&& Objects.equals(overdue, that.overdue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(effectuated, paymentDate, detail, deadline, overdue);
	}

	@Override
	public String toString() {
		return "PaymentStatusResponse{effectuated=" + effectuated + ", paymentDate='" + paymentDate + "', detail='" + detail + "', deadline='" + deadline + "', overdue=" + overdue + "}";
	}
}
