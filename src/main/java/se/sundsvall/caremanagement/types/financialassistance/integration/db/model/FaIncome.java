package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class FaIncome {

	@Column(name = "income_type")
	private String incomeType;

	@Column(name = "amount", precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "income_date")
	private LocalDate incomeDate;

	@Column(name = "recipient")
	private String recipient;

	public static FaIncome create() {
		return new FaIncome();
	}

	public String getIncomeType() {
		return incomeType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDate getIncomeDate() {
		return incomeDate;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setIncomeType(final String incomeType) {
		this.incomeType = incomeType;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public void setIncomeDate(final LocalDate incomeDate) {
		this.incomeDate = incomeDate;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public FaIncome withIncomeType(final String incomeType) {
		this.incomeType = incomeType;
		return this;
	}

	public FaIncome withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public FaIncome withIncomeDate(final LocalDate incomeDate) {
		this.incomeDate = incomeDate;
		return this;
	}

	public FaIncome withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaIncome that = (FaIncome) o;
		return Objects.equals(incomeType, that.incomeType) && Objects.equals(amount, that.amount)
			&& Objects.equals(incomeDate, that.incomeDate) && Objects.equals(recipient, that.recipient);
	}

	@Override
	public int hashCode() {
		return Objects.hash(incomeType, amount, incomeDate, recipient);
	}

	@Override
	public String toString() {
		return "FaIncome{incomeType='" + incomeType + "', amount=" + amount + ", incomeDate=" + incomeDate
			+ ", recipient='" + recipient + "'}";
	}
}
