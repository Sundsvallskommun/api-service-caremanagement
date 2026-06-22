package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "An income reported by the applicant or co-applicant.")
public class Income {

	@Schema(
		description = "The type of income — the complete handläggare set; SSBTEK-sourced types are handläggare-only, the rest are reportable in Mina sidor (see GET .../errands/financial-assistance/metadata for the labelled catalogue + citizenReportable flag)",
		examples = "SALARY_AFTER_TAX",
		allowableValues = {
			"UNEMPLOYMENT_BENEFIT", "UNEMPLOYMENT_OR_ALPHA_BENEFIT", "ACTIVITY_COMPENSATION", "ACTIVITY_SUPPORT", "ALPHA_BENEFIT", "CHILD_ALLOWANCE", "CHILD_PENSION", "HOUSING_ALLOWANCE", "HOUSING_SUPPLEMENT", "CSN_GRANT", "CSN_LOAN", "DAILY_ALLOWANCE_FK",
			"SURVIVOR_SUPPORT", "FINANCIAL_AID_OTHER_MUNICIPALITY", "ESTABLISHMENT_BENEFIT", "PARENTAL_BENEFIT", "RENT_SHARE_FROM_CHILD", "LODGING_ALLOWANCE", "CAPITAL_INCOME", "SALARY_AFTER_TAX", "PENSION", "PENSION_ANNUITY_CARE", "SICKNESS_COMPENSATION",
			"SICKNESS_BENEFIT", "TAX_REFUND", "SWISH_DEPOSITS_TRANSFERS", "OCCUPATIONAL_PENSION_INSURANCE", "CHILD_SUPPORT", "MAINTENANCE_SUPPORT", "CARE_ALLOWANCE", "ELDERLY_SUPPORT", "SURPLUS_FROM_PREVIOUS_MONTH", "OTHER_INCOME"
		})
	@OneOf(value = {
		"UNEMPLOYMENT_BENEFIT", "UNEMPLOYMENT_OR_ALPHA_BENEFIT", "ACTIVITY_COMPENSATION", "ACTIVITY_SUPPORT", "ALPHA_BENEFIT", "CHILD_ALLOWANCE", "CHILD_PENSION", "HOUSING_ALLOWANCE", "HOUSING_SUPPLEMENT", "CSN_GRANT", "CSN_LOAN", "DAILY_ALLOWANCE_FK",
		"SURVIVOR_SUPPORT", "FINANCIAL_AID_OTHER_MUNICIPALITY", "ESTABLISHMENT_BENEFIT", "PARENTAL_BENEFIT", "RENT_SHARE_FROM_CHILD", "LODGING_ALLOWANCE", "CAPITAL_INCOME", "SALARY_AFTER_TAX", "PENSION", "PENSION_ANNUITY_CARE", "SICKNESS_COMPENSATION",
		"SICKNESS_BENEFIT", "TAX_REFUND", "SWISH_DEPOSITS_TRANSFERS", "OCCUPATIONAL_PENSION_INSURANCE", "CHILD_SUPPORT", "MAINTENANCE_SUPPORT", "CARE_ALLOWANCE", "ELDERLY_SUPPORT", "SURPLUS_FROM_PREVIOUS_MONTH", "OTHER_INCOME"
	}, nullable = true)
	private String incomeType;

	@Schema(description = "The income amount", examples = "18500.00")
	private BigDecimal amount;

	@Schema(description = "The date the income was received", examples = "2026-05-25")
	private LocalDate incomeDate;

	@Schema(description = "Who received the income", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	})
	@OneOf(value = {
		"APPLICANT", "CO_APPLICANT"
	}, nullable = true)
	private String recipient;

	public static Income create() {
		return new Income();
	}

	public String getIncomeType() {
		return incomeType;
	}

	public void setIncomeType(final String incomeType) {
		this.incomeType = incomeType;
	}

	public Income withIncomeType(final String incomeType) {
		this.incomeType = incomeType;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public Income withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public LocalDate getIncomeDate() {
		return incomeDate;
	}

	public void setIncomeDate(final LocalDate incomeDate) {
		this.incomeDate = incomeDate;
	}

	public Income withIncomeDate(final LocalDate incomeDate) {
		this.incomeDate = incomeDate;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public Income withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Income that = (Income) o;
		return Objects.equals(incomeType, that.incomeType) && Objects.equals(amount, that.amount)
			&& Objects.equals(incomeDate, that.incomeDate) && Objects.equals(recipient, that.recipient);
	}

	@Override
	public int hashCode() {
		return Objects.hash(incomeType, amount, incomeDate, recipient);
	}

	@Override
	public String toString() {
		return "Income{incomeType='" + incomeType + "', amount=" + amount + ", incomeDate=" + incomeDate
			+ ", recipient='" + recipient + "'}";
	}
}
