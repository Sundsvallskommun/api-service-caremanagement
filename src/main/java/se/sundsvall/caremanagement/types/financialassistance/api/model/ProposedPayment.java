package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One proposed payment. The proposal returns a list of these (one entry) so the frontend can split the bistånd into
 * several payments before it is registered.
 */
@Schema(description = "One proposed payment (a proposal always starts with a single entry; the frontend may split it).")
public class ProposedPayment {

	@Schema(
		description = "The proposed payment date: the 27th of the concerned month, moved to the Friday before when the 27th is a Saturday (→ 26th) or Sunday (→ 25th). Swedish public holidays (röda dagar) are not considered — an open question with verksamheten",
		examples = "2026-06-26")
	private LocalDate paymentDate;

	@Schema(description = "The proposed amount — the whole estimated bistånd (see DecisionProposal.estimatedAmount). Null when no norm is known", examples = "4250")
	private BigDecimal amount;

	@Schema(description = "The month the payment concerns (YYYY-MM) — the calculation's application month", examples = "2026-06")
	private String concernedMonth;

	@Schema(description = "The proposed payee, or null when neither a previous payment nor the application names one", implementation = Payee.class)
	private Payee payee;

	@Schema(description = "Kontering. Always null for now: the FamilyCare API exposes no accounting code, so the caseworker fills it in. The field exists so the frontend can render and post it", nullable = true)
	private String accountingCode;

	public static ProposedPayment create() {
		return new ProposedPayment();
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public ProposedPayment withPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public ProposedPayment withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getConcernedMonth() {
		return concernedMonth;
	}

	public void setConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
	}

	public ProposedPayment withConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
		return this;
	}

	public Payee getPayee() {
		return payee;
	}

	public void setPayee(final Payee payee) {
		this.payee = payee;
	}

	public ProposedPayment withPayee(final Payee payee) {
		this.payee = payee;
		return this;
	}

	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public ProposedPayment withAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ProposedPayment that = (ProposedPayment) o;
		return Objects.equals(paymentDate, that.paymentDate) && Objects.equals(amount, that.amount) && Objects.equals(concernedMonth, that.concernedMonth)
			&& Objects.equals(payee, that.payee) && Objects.equals(accountingCode, that.accountingCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(paymentDate, amount, concernedMonth, payee, accountingCode);
	}

	@Override
	public String toString() {
		return "ProposedPayment{" +
			"paymentDate=" + paymentDate +
			", amount=" + amount +
			", concernedMonth='" + concernedMonth + '\'' +
			", payee=" + payee +
			", accountingCode='" + accountingCode + '\'' +
			'}';
	}
}
