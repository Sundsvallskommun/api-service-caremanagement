package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Lifecare's summering of a beräkning, signed the way the caseworker reads it: norm, utgifter and levnadskostnader i
 * övrigt are positive and subtracted; result is the överskott (positive) or underskott (negative).
 */
@Schema(description = "Lifecare's summering of a beräkning.")
public class LifecareCalculationSummary {

	@Schema(description = "The incomes")
	private BigDecimal income;

	@Schema(description = "Jobbstimulans gross: the income it is counted on")
	private BigDecimal jobStimulus;

	@Schema(description = "The part of the income jobbstimulans leaves out")
	private BigDecimal jobStimulusDeduction;

	@Schema(description = "The norm")
	private BigDecimal norm;

	@Schema(description = "The norm part for the members")
	private BigDecimal familyCost;

	@Schema(description = "The norm part for the gemensamma kostnader")
	private BigDecimal commonHouseholdCost;

	@Schema(description = "The utgifter")
	private BigDecimal expenses;

	@Schema(description = "Incomes minus norm minus utgifter")
	private BigDecimal sum;

	@Schema(description = "The levnadskostnader i övrigt")
	private BigDecimal specialExpenses;

	@Schema(description = "The överskott (positive) or underskott (negative)")
	private BigDecimal result;

	public static LifecareCalculationSummary create() {
		return new LifecareCalculationSummary();
	}

	public BigDecimal getIncome() {
		return income;
	}

	public void setIncome(final BigDecimal income) {
		this.income = income;
	}

	public LifecareCalculationSummary withIncome(final BigDecimal income) {
		this.income = income;
		return this;
	}

	public BigDecimal getJobStimulus() {
		return jobStimulus;
	}

	public void setJobStimulus(final BigDecimal jobStimulus) {
		this.jobStimulus = jobStimulus;
	}

	public LifecareCalculationSummary withJobStimulus(final BigDecimal jobStimulus) {
		this.jobStimulus = jobStimulus;
		return this;
	}

	public BigDecimal getJobStimulusDeduction() {
		return jobStimulusDeduction;
	}

	public void setJobStimulusDeduction(final BigDecimal jobStimulusDeduction) {
		this.jobStimulusDeduction = jobStimulusDeduction;
	}

	public LifecareCalculationSummary withJobStimulusDeduction(final BigDecimal jobStimulusDeduction) {
		this.jobStimulusDeduction = jobStimulusDeduction;
		return this;
	}

	public BigDecimal getNorm() {
		return norm;
	}

	public void setNorm(final BigDecimal norm) {
		this.norm = norm;
	}

	public LifecareCalculationSummary withNorm(final BigDecimal norm) {
		this.norm = norm;
		return this;
	}

	public BigDecimal getFamilyCost() {
		return familyCost;
	}

	public void setFamilyCost(final BigDecimal familyCost) {
		this.familyCost = familyCost;
	}

	public LifecareCalculationSummary withFamilyCost(final BigDecimal familyCost) {
		this.familyCost = familyCost;
		return this;
	}

	public BigDecimal getCommonHouseholdCost() {
		return commonHouseholdCost;
	}

	public void setCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
	}

	public LifecareCalculationSummary withCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
		return this;
	}

	public BigDecimal getExpenses() {
		return expenses;
	}

	public void setExpenses(final BigDecimal expenses) {
		this.expenses = expenses;
	}

	public LifecareCalculationSummary withExpenses(final BigDecimal expenses) {
		this.expenses = expenses;
		return this;
	}

	public BigDecimal getSum() {
		return sum;
	}

	public void setSum(final BigDecimal sum) {
		this.sum = sum;
	}

	public LifecareCalculationSummary withSum(final BigDecimal sum) {
		this.sum = sum;
		return this;
	}

	public BigDecimal getSpecialExpenses() {
		return specialExpenses;
	}

	public void setSpecialExpenses(final BigDecimal specialExpenses) {
		this.specialExpenses = specialExpenses;
	}

	public LifecareCalculationSummary withSpecialExpenses(final BigDecimal specialExpenses) {
		this.specialExpenses = specialExpenses;
		return this;
	}

	public BigDecimal getResult() {
		return result;
	}

	public void setResult(final BigDecimal result) {
		this.result = result;
	}

	public LifecareCalculationSummary withResult(final BigDecimal result) {
		this.result = result;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareCalculationSummary that = (LifecareCalculationSummary) o;
		return Objects.equals(income, that.income) && Objects.equals(jobStimulus, that.jobStimulus) && Objects.equals(jobStimulusDeduction, that.jobStimulusDeduction) && Objects.equals(norm, that.norm) && Objects.equals(familyCost, that.familyCost)
			&& Objects.equals(commonHouseholdCost, that.commonHouseholdCost) && Objects.equals(expenses, that.expenses) && Objects.equals(sum, that.sum) && Objects.equals(specialExpenses, that.specialExpenses) && Objects.equals(result, that.result);
	}

	@Override
	public int hashCode() {
		return Objects.hash(income, jobStimulus, jobStimulusDeduction, norm, familyCost, commonHouseholdCost, expenses, sum, specialExpenses, result);
	}

	@Override
	public String toString() {
		return "LifecareCalculationSummary{" +
			"income=" + income +
			", jobStimulus=" + jobStimulus +
			", jobStimulusDeduction=" + jobStimulusDeduction +
			", norm=" + norm +
			", familyCost=" + familyCost +
			", commonHouseholdCost=" + commonHouseholdCost +
			", expenses=" + expenses +
			", sum=" + sum +
			", specialExpenses=" + specialExpenses +
			", result=" + result +
			'}';
	}
}
