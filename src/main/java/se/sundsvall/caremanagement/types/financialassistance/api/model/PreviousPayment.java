package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * The applicant's most recent Lifecare payment — the baseline the payment proposal starts from (same payee, same
 * message). Dates and the concerned month are the raw Lifecare strings.
 */
@Schema(description = "The applicant's most recent Lifecare payment.")
public class PreviousPayment {

	@Schema(description = "The Lifecare pay date (raw Lifecare string)", examples = "2026-05-27")
	private String payDate;

	@Schema(description = "The paid amount", examples = "8500")
	private BigDecimal amount;

	@Schema(description = "The month the payment concerned (raw Lifecare string)", examples = "2026-06")
	private String concernedMonth;

	@Schema(description = "The payment method as Lifecare names it", examples = "Bankkonto")
	private String paymentMethod;

	@Schema(description = "The payee name", examples = "Anna Andersson")
	private String name;

	@Schema(description = "The clearing number, when a bank account", examples = "1234")
	private String clearing;

	@Schema(description = "The account number, when a bank account", examples = "5678901")
	private String accountNumber;

	@Schema(description = "The payment message", examples = "Ekonomiskt bistånd juni")
	private String message;

	public static PreviousPayment create() {
		return new PreviousPayment();
	}

	public String getPayDate() {
		return payDate;
	}

	public void setPayDate(final String payDate) {
		this.payDate = payDate;
	}

	public PreviousPayment withPayDate(final String payDate) {
		this.payDate = payDate;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public PreviousPayment withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getConcernedMonth() {
		return concernedMonth;
	}

	public void setConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
	}

	public PreviousPayment withConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public PreviousPayment withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public PreviousPayment withName(final String name) {
		this.name = name;
		return this;
	}

	public String getClearing() {
		return clearing;
	}

	public void setClearing(final String clearing) {
		this.clearing = clearing;
	}

	public PreviousPayment withClearing(final String clearing) {
		this.clearing = clearing;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public PreviousPayment withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public PreviousPayment withMessage(final String message) {
		this.message = message;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PreviousPayment that = (PreviousPayment) o;
		return Objects.equals(payDate, that.payDate) && Objects.equals(amount, that.amount) && Objects.equals(concernedMonth, that.concernedMonth)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(name, that.name) && Objects.equals(clearing, that.clearing)
			&& Objects.equals(accountNumber, that.accountNumber) && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(payDate, amount, concernedMonth, paymentMethod, name, clearing, accountNumber, message);
	}

	@Override
	public String toString() {
		return "PreviousPayment{" +
			"payDate='" + payDate + '\'' +
			", amount=" + amount +
			", concernedMonth='" + concernedMonth + '\'' +
			", paymentMethod='" + paymentMethod + '\'' +
			", name='" + name + '\'' +
			", clearing='" + clearing + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			", message='" + message + '\'' +
			'}';
	}
}
