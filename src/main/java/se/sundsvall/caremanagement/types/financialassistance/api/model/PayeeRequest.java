package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * A betalningsmottagare the caseworker adds by hand on an errand.
 *
 * <p>
 * Free-form on purpose: which fields a payment method actually needs is Lifecare's rule, not
 * ours, and verksamheten was explicit that the form should not encode per-betalsätt field logic. {@code clearing} and
 * {@code accountNumber} are therefore optional independently — Lifecare gets what has a value, and the report back
 * carries
 * Lifecare's own complaint when the combination is rejected.
 * </p>
 */
@Schema(description = "A betalningsmottagare to add by hand on an errand.")
public class PayeeRequest {

	@Schema(description = "Name of the payee, as it should read in Lifecare", examples = "Sundsvalls Hyresbostäder AB", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String name;

	@Schema(description = "The Lifecare payment method", examples = "Bankgiro via Plusgiro", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 64)
	private String paymentMethod;

	@Schema(description = "Clearing number, when the payment method needs one", examples = "6000")
	@Size(max = 16)
	private String clearing;

	@Schema(description = "Account, bankgiro or plusgiro number, when the payment method needs one", examples = "5555-6666")
	@Size(max = 64)
	private String accountNumber;

	public static PayeeRequest create() {
		return new PayeeRequest();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public PayeeRequest withName(final String name) {
		this.name = name;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public PayeeRequest withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getClearing() {
		return clearing;
	}

	public void setClearing(final String clearing) {
		this.clearing = clearing;
	}

	public PayeeRequest withClearing(final String clearing) {
		this.clearing = clearing;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public PayeeRequest withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PayeeRequest that = (PayeeRequest) o;
		return Objects.equals(name, that.name) && Objects.equals(paymentMethod, that.paymentMethod)
			&& Objects.equals(clearing, that.clearing) && Objects.equals(accountNumber, that.accountNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, paymentMethod, clearing, accountNumber);
	}

	@Override
	public String toString() {
		return "PayeeRequest{" +
			"name='" + name + '\'' +
			", paymentMethod='" + paymentMethod + '\'' +
			", clearing='" + clearing + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			'}';
	}
}
