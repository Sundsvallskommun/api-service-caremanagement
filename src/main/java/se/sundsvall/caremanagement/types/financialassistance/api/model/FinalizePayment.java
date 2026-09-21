package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * One utbetalning the robot registers in Lifecare ({@code REGISTER_PAYMENT}). A granting decision may be paid out in
 * several payments (e.g. rent to the landlord and the rest to the applicant), so the finalize request carries a list.
 */
@Schema(description = "One payment to register in Lifecare for a granting decision.")
public class FinalizePayment {

	@Schema(description = "The date the payment is to be made", examples = "2026-06-25", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@DateTimeFormat(iso = DATE)
	private LocalDate paymentDate;

	@Schema(description = "The payment amount in SEK", examples = "7900.00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Positive
	private BigDecimal amount;

	@Schema(description = "The month the payment concerns (ISO year-month, yyyy-MM) — the month the process polls Lifecare payments for", examples = "2026-06", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be an ISO year-month (yyyy-MM)")
	private String concernedMonth;

	@Schema(description = "Who the payment goes to and how", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Valid
	private Payee payee;

	@Schema(description = "Optional accounting code (kontering) for the payment", examples = "5011")
	@Size(max = 64)
	private String accountingCode;

	public static FinalizePayment create() {
		return new FinalizePayment();
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public FinalizePayment withPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public FinalizePayment withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getConcernedMonth() {
		return concernedMonth;
	}

	public void setConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
	}

	public FinalizePayment withConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
		return this;
	}

	public Payee getPayee() {
		return payee;
	}

	public void setPayee(final Payee payee) {
		this.payee = payee;
	}

	public FinalizePayment withPayee(final Payee payee) {
		this.payee = payee;
		return this;
	}

	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public FinalizePayment withAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinalizePayment that = (FinalizePayment) o;
		return Objects.equals(paymentDate, that.paymentDate) && Objects.equals(amount, that.amount) && Objects.equals(concernedMonth, that.concernedMonth)
			&& Objects.equals(payee, that.payee) && Objects.equals(accountingCode, that.accountingCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(paymentDate, amount, concernedMonth, payee, accountingCode);
	}

	@Override
	public String toString() {
		return "FinalizePayment{" +
			"paymentDate=" + paymentDate +
			", amount=" + amount +
			", concernedMonth='" + concernedMonth + '\'' +
			", payee=" + payee +
			", accountingCode='" + accountingCode + '\'' +
			'}';
	}
}
