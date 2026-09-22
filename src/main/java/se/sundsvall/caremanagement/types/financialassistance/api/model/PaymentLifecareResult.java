package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/**
 * The {@code REGISTER_PAYMENT} robot's report on what happened when it tried to register a decided payment in
 * Lifecare. The counterpart of {@link PayeeLifecareResult}, and deliberately the same shape: outcome, the id Lifecare
 * gave, and Lifecare's own message.
 *
 * <p>
 * Without this report a decided payment stays {@code PENDING_REGISTRATION} for ever, whatever the robot managed to do
 * — the status could say no more than that it was decided and the Lifecare state unknown.
 * </p>
 *
 * <p>
 * {@code REGISTERED} means the payment now exists in Lifecare, not that it has been paid out. Whether it has been
 * effectuated is a separate question the process asks through {@code POST .../financial-assistance/payment-status};
 * this report is only about the registration the robot performed.
 * </p>
 *
 * <p>
 * {@code ALREADY_EXISTS} counts as success, as it does for a payee. It is expected to be rare here: Lifecare offers no
 * way to ask whether a payment has already been registered, so the robot can usually only report it when it happens to
 * recognise the row. {@code detail} is required on {@code FAILED} and must be what Lifecare actually said, because
 * that text is what the caseworker gets to see.
 * </p>
 */
@Schema(description = "The REGISTER_PAYMENT robot's report on registering a payment in Lifecare.")
public class PaymentLifecareResult {

	@Schema(description = "What the robot ended up doing", examples = "REGISTERED", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
		"REGISTERED", "ALREADY_EXISTS", "FAILED"
	})
	@NotBlank
	@OneOf({
		"REGISTERED", "ALREADY_EXISTS", "FAILED"
	})
	private String outcome;

	@Schema(description = "The payment id Lifecare gave, when it gave one — what CreatePaymentForService returns. Stored as the "
		+ "payment's lifecareId", examples = "4")
	@Size(max = 64)
	private String lifecarePaymentId;

	@Schema(description = "Lifecare's own message. Required when outcome is FAILED — it is shown to the caseworker as-is",
		examples = "Betalningsmottagaren saknas i Lifecare")
	@Size(max = 1024)
	private String detail;

	public static PaymentLifecareResult create() {
		return new PaymentLifecareResult();
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public PaymentLifecareResult withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getLifecarePaymentId() {
		return lifecarePaymentId;
	}

	public void setLifecarePaymentId(final String lifecarePaymentId) {
		this.lifecarePaymentId = lifecarePaymentId;
	}

	public PaymentLifecareResult withLifecarePaymentId(final String lifecarePaymentId) {
		this.lifecarePaymentId = lifecarePaymentId;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(final String detail) {
		this.detail = detail;
	}

	public PaymentLifecareResult withDetail(final String detail) {
		this.detail = detail;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PaymentLifecareResult that = (PaymentLifecareResult) o;
		return Objects.equals(outcome, that.outcome) && Objects.equals(lifecarePaymentId, that.lifecarePaymentId)
			&& Objects.equals(detail, that.detail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outcome, lifecarePaymentId, detail);
	}

	@Override
	public String toString() {
		return "PaymentLifecareResult{" +
			"outcome='" + outcome + '\'' +
			", lifecarePaymentId='" + lifecarePaymentId + '\'' +
			", detail='" + detail + '\'' +
			'}';
	}
}
