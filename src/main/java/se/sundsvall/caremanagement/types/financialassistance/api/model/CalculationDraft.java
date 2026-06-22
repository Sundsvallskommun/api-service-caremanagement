package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * The full draft calculation for an errand — header (month + selected norm), the three sections (persons, incomes,
 * expenses) and the section sums. persons give the norm base, incomes are subtracted, expenses added.
 */
@Schema(description = "The full draft calculation — header, the three sections (persons, incomes, expenses) and the section sums.")
public class CalculationDraft {

	@Schema(description = "The errand id", accessMode = Schema.AccessMode.READ_ONLY)
	private String errandId;

	@Schema(description = "The application month (ISO yyyy-MM)", examples = "2026-06")
	private String applicationMonth;

	@Schema(description = "The selected norm id")
	private Integer normId;

	@Schema(description = "The selected norm type")
	private String normType;

	@Schema(description = "The start date of the calculation period")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationFromDate;

	@Schema(description = "The end date of the calculation period")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationToDate;

	@Schema(description = "The date the calculation is performed")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationDate;

	@Schema(description = "Whether the household size has been overridden by a caseworker")
	private Boolean hasCustomHouseholdSize;

	@Schema(description = "The household size used for the norm", examples = "3")
	private Integer householdSize;

	@Schema(description = "The person rows (persons)")
	private List<NormPersonRow> persons = new ArrayList<>();

	@Schema(description = "The income rows (incomes)")
	private List<NormIncomeRow> incomes = new ArrayList<>();

	@Schema(description = "The expense rows (expenses)")
	private List<NormExpenseRow> expenses = new ArrayList<>();

	@Schema(description = "The special expense rows")
	private List<NormExpenseRow> specialExpenses = new ArrayList<>();

	@Schema(description = "The sum of the effective income amounts", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal incomeSum;

	@Schema(description = "The sum of the effective expense amounts", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal expenseSum;

	@Schema(description = "The sum of the effective special expense amounts", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal specialExpenseSum;

	@Schema(description = "When the draft was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the draft was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static CalculationDraft create() {
		return new CalculationDraft();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public CalculationDraft withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public CalculationDraft withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public Integer getNormId() {
		return normId;
	}

	public void setNormId(final Integer normId) {
		this.normId = normId;
	}

	public CalculationDraft withNormId(final Integer normId) {
		this.normId = normId;
		return this;
	}

	public String getNormType() {
		return normType;
	}

	public void setNormType(final String normType) {
		this.normType = normType;
	}

	public CalculationDraft withNormType(final String normType) {
		this.normType = normType;
		return this;
	}

	public LocalDate getCalculationFromDate() {
		return calculationFromDate;
	}

	public void setCalculationFromDate(final LocalDate calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
	}

	public CalculationDraft withCalculationFromDate(final LocalDate calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
		return this;
	}

	public LocalDate getCalculationToDate() {
		return calculationToDate;
	}

	public void setCalculationToDate(final LocalDate calculationToDate) {
		this.calculationToDate = calculationToDate;
	}

	public CalculationDraft withCalculationToDate(final LocalDate calculationToDate) {
		this.calculationToDate = calculationToDate;
		return this;
	}

	public LocalDate getCalculationDate() {
		return calculationDate;
	}

	public void setCalculationDate(final LocalDate calculationDate) {
		this.calculationDate = calculationDate;
	}

	public CalculationDraft withCalculationDate(final LocalDate calculationDate) {
		this.calculationDate = calculationDate;
		return this;
	}

	public Boolean getHasCustomHouseholdSize() {
		return hasCustomHouseholdSize;
	}

	public void setHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
	}

	public CalculationDraft withHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
		return this;
	}

	public Integer getHouseholdSize() {
		return householdSize;
	}

	public void setHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
	}

	public CalculationDraft withHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
		return this;
	}

	public List<NormPersonRow> getPersons() {
		return persons;
	}

	public void setPersons(final List<NormPersonRow> persons) {
		this.persons = persons;
	}

	public CalculationDraft withPersons(final List<NormPersonRow> persons) {
		this.persons = persons;
		return this;
	}

	public List<NormIncomeRow> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<NormIncomeRow> incomes) {
		this.incomes = incomes;
	}

	public CalculationDraft withIncomes(final List<NormIncomeRow> incomes) {
		this.incomes = incomes;
		return this;
	}

	public List<NormExpenseRow> getExpenses() {
		return expenses;
	}

	public void setExpenses(final List<NormExpenseRow> expenses) {
		this.expenses = expenses;
	}

	public CalculationDraft withExpenses(final List<NormExpenseRow> expenses) {
		this.expenses = expenses;
		return this;
	}

	public List<NormExpenseRow> getSpecialExpenses() {
		return specialExpenses;
	}

	public void setSpecialExpenses(final List<NormExpenseRow> specialExpenses) {
		this.specialExpenses = specialExpenses;
	}

	public CalculationDraft withSpecialExpenses(final List<NormExpenseRow> specialExpenses) {
		this.specialExpenses = specialExpenses;
		return this;
	}

	public BigDecimal getIncomeSum() {
		return incomeSum;
	}

	public void setIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
	}

	public CalculationDraft withIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
		return this;
	}

	public BigDecimal getExpenseSum() {
		return expenseSum;
	}

	public void setExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
	}

	public CalculationDraft withExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
		return this;
	}

	public BigDecimal getSpecialExpenseSum() {
		return specialExpenseSum;
	}

	public void setSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
	}

	public CalculationDraft withSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public CalculationDraft withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public CalculationDraft withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CalculationDraft that = (CalculationDraft) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(normId, that.normId)
			&& Objects.equals(normType, that.normType) && Objects.equals(calculationFromDate, that.calculationFromDate)
			&& Objects.equals(calculationToDate, that.calculationToDate) && Objects.equals(calculationDate, that.calculationDate)
			&& Objects.equals(hasCustomHouseholdSize, that.hasCustomHouseholdSize) && Objects.equals(householdSize, that.householdSize)
			&& Objects.equals(persons, that.persons) && Objects.equals(incomes, that.incomes) && Objects.equals(expenses, that.expenses)
			&& Objects.equals(specialExpenses, that.specialExpenses) && Objects.equals(incomeSum, that.incomeSum) && Objects.equals(expenseSum, that.expenseSum)
			&& Objects.equals(specialExpenseSum, that.specialExpenseSum) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, normId, normType, calculationFromDate, calculationToDate, calculationDate, hasCustomHouseholdSize, householdSize,
			persons, incomes, expenses, specialExpenses, incomeSum, expenseSum, specialExpenseSum, created, updated);
	}

	@Override
	public String toString() {
		return "CalculationDraft{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", normId=" + normId +
			", normType='" + normType + '\'' +
			", calculationFromDate=" + calculationFromDate +
			", calculationToDate=" + calculationToDate +
			", calculationDate=" + calculationDate +
			", hasCustomHouseholdSize=" + hasCustomHouseholdSize +
			", householdSize=" + householdSize +
			", persons=" + persons +
			", incomes=" + incomes +
			", expenses=" + expenses +
			", specialExpenses=" + specialExpenses +
			", incomeSum=" + incomeSum +
			", expenseSum=" + expenseSum +
			", specialExpenseSum=" + specialExpenseSum +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
