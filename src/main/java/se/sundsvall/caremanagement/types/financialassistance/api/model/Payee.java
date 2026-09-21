package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Who a payment goes to and how. Free-form on purpose: the payment methods and account formats are Lifecare's, and
 * the robot types them into the Lifecare payment form as given.
 */
@Schema(description = "The recipient of a payment and the payment method.")
public class Payee {

	@Schema(description = "Name of the payee as registered in Lifecare", examples = "Anna Andersson", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String name;

	@Schema(description = "The Lifecare payment method, e.g. bank account, bankgiro, plusgiro or utbetalningskort", examples = "BANKKONTO", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 64)
	private String paymentMethod;

	@Schema(description = "Clearing number, when the payment method needs one", examples = "6000")
	@Size(max = 16)
	private String clearing;

	@Schema(description = "Account, bankgiro or plusgiro number, when the payment method needs one", examples = "123456789")
	@Size(max = 64)
	private String accountNumber;

	public static Payee create() {
		return new Payee();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Payee withName(final String name) {
		this.name = name;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public Payee withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getClearing() {
		return clearing;
	}

	public void setClearing(final String clearing) {
		this.clearing = clearing;
	}

	public Payee withClearing(final String clearing) {
		this.clearing = clearing;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public Payee withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Payee that = (Payee) o;
		return Objects.equals(name, that.name) && Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(clearing, that.clearing)
			&& Objects.equals(accountNumber, that.accountNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, paymentMethod, clearing, accountNumber);
	}

	@Override
	public String toString() {
		return "Payee{" +
			"name='" + name + '\'' +
			", paymentMethod='" + paymentMethod + '\'' +
			", clearing='" + clearing + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			'}';
	}
}
