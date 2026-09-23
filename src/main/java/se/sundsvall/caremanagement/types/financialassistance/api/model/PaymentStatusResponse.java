package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Result of reading the Lifecare payment status for an application month: whether it has been effectuated, when it
 * has the payment date, and when it has not, why.
 */
@Schema(description = "Whether the Lifecare payment for the application month has been effectuated.")
public class PaymentStatusResponse {

	@Schema(description = "True when the decided payments (or, without an errand, a payment for the application month) are registered in Lifecare", examples = "true")
	private Boolean effectuated;

	@Schema(description = "The date the payment was made (Lifecare PayDate), when effectuated", examples = "2026-05-27")
	private String paymentDate;

	@Schema(description = "Why the status is not effectuated, in words a caseworker can act on; empty when effectuated", examples = "1 av 2 beslutade utbetalningar är inte registrerade i Lifecare")
	private String detail;

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

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PaymentStatusResponse that = (PaymentStatusResponse) o;
		return Objects.equals(effectuated, that.effectuated) && Objects.equals(paymentDate, that.paymentDate) && Objects.equals(detail, that.detail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(effectuated, paymentDate, detail);
	}

	@Override
	public String toString() {
		return "PaymentStatusResponse{effectuated=" + effectuated + ", paymentDate='" + paymentDate + "', detail='" + detail + "'}";
	}
}
