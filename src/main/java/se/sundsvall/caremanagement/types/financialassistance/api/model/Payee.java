package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A payment recipient — the name plus account details the caseworker needs to register the payment. Never carries a
 * personal number.
 */
@Schema(description = "A payment recipient — name and account details.")
public class Payee {

	@Schema(description = "The payee name as registered on the payment", examples = "Anna Andersson")
	private String name;

	@Schema(description = "The payment method as Lifecare names it (free text)", examples = "Bankkonto")
	private String paymentMethod;

	@Schema(description = "The clearing number, when the method is a bank account", examples = "1234")
	private String clearing;

	@Schema(description = "The account number, when the method is a bank account", examples = "5678901")
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
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
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
