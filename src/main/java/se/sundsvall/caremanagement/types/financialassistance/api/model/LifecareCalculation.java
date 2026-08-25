package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A Lifecare calculation with the full breakdown — sums, household members, incomes and expenses — the read model the
 * frontend renders so a caseworker can review the calculation behind a decision.
 */
@Schema(description = "A Lifecare calculation, full breakdown.")
public class LifecareCalculation {

	@Schema(description = "The Lifecare calculation id", examples = "7001")
	private Integer id;

	@Schema(description = "The norm the calculation is based on", examples = "Riksnorm 2026")
	private String norm;

	@Schema(description = "The start date of the calculation period", examples = "2026-06-01")
	private String fromDate;

	@Schema(description = "The end date of the calculation period", examples = "2026-06-30")
	private String toDate;

	@Schema(description = "The sum of all incomes", examples = "12000.0")
	private BigDecimal incomeSum;

	@Schema(description = "The sum of all regular expenses", examples = "9500.0")
	private BigDecimal expenseSum;

	@Schema(description = "The sum of all special expenses", examples = "500.0")
	private BigDecimal specialExpenseSum;

	@Schema(description = "The sum of the norm", examples = "10500.0")
	private BigDecimal normSum;

	@Schema(description = "The common household cost", examples = "1200.0")
	private BigDecimal commonHouseholdCost;

	@Schema(description = "The family cost", examples = "800.0")
	private BigDecimal familyCost;

	@Schema(description = "The balance of the calculation", examples = "-2000.0")
	private BigDecimal balance;

	@Schema(description = "The total sum of the calculation", examples = "8500.0")
	private BigDecimal totalSum;

	@Schema(description = "Whether the calculation is final", examples = "true")
	private Boolean isFinal;

	@ArraySchema(schema = @Schema(implementation = LifecareCalculationPerson.class))
	private List<LifecareCalculationPerson> persons;

	@ArraySchema(schema = @Schema(implementation = LifecareCalculationIncome.class))
	private List<LifecareCalculationIncome> incomes;

	@ArraySchema(schema = @Schema(implementation = LifecareCalculationExpense.class))
	private List<LifecareCalculationExpense> expenses;

	@ArraySchema(schema = @Schema(implementation = LifecareCalculationExpense.class))
	private List<LifecareCalculationExpense> specialExpenses;

	public static LifecareCalculation create() {
		return new LifecareCalculation();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public LifecareCalculation withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getNorm() {
		return norm;
	}

	public void setNorm(final String norm) {
		this.norm = norm;
	}

	public LifecareCalculation withNorm(final String norm) {
		this.norm = norm;
		return this;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(final String fromDate) {
		this.fromDate = fromDate;
	}

	public LifecareCalculation withFromDate(final String fromDate) {
		this.fromDate = fromDate;
		return this;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(final String toDate) {
		this.toDate = toDate;
	}

	public LifecareCalculation withToDate(final String toDate) {
		this.toDate = toDate;
		return this;
	}

	public BigDecimal getIncomeSum() {
		return incomeSum;
	}

	public void setIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
	}

	public LifecareCalculation withIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
		return this;
	}

	public BigDecimal getExpenseSum() {
		return expenseSum;
	}

	public void setExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
	}

	public LifecareCalculation withExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
		return this;
	}

	public BigDecimal getSpecialExpenseSum() {
		return specialExpenseSum;
	}

	public void setSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
	}

	public LifecareCalculation withSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
		return this;
	}

	public BigDecimal getNormSum() {
		return normSum;
	}

	public void setNormSum(final BigDecimal normSum) {
		this.normSum = normSum;
	}

	public LifecareCalculation withNormSum(final BigDecimal normSum) {
		this.normSum = normSum;
		return this;
	}

	public BigDecimal getCommonHouseholdCost() {
		return commonHouseholdCost;
	}

	public void setCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
	}

	public LifecareCalculation withCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
		return this;
	}

	public BigDecimal getFamilyCost() {
		return familyCost;
	}

	public void setFamilyCost(final BigDecimal familyCost) {
		this.familyCost = familyCost;
	}

	public LifecareCalculation withFamilyCost(final BigDecimal familyCost) {
		this.familyCost = familyCost;
		return this;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(final BigDecimal balance) {
		this.balance = balance;
	}

	public LifecareCalculation withBalance(final BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public BigDecimal getTotalSum() {
		return totalSum;
	}

	public void setTotalSum(final BigDecimal totalSum) {
		this.totalSum = totalSum;
	}

	public LifecareCalculation withTotalSum(final BigDecimal totalSum) {
		this.totalSum = totalSum;
		return this;
	}

	public Boolean getIsFinal() {
		return isFinal;
	}

	public void setIsFinal(final Boolean isFinal) {
		this.isFinal = isFinal;
	}

	public LifecareCalculation withIsFinal(final Boolean isFinal) {
		this.isFinal = isFinal;
		return this;
	}

	public List<LifecareCalculationPerson> getPersons() {
		return persons;
	}

	public void setPersons(final List<LifecareCalculationPerson> persons) {
		this.persons = persons;
	}

	public LifecareCalculation withPersons(final List<LifecareCalculationPerson> persons) {
		this.persons = persons;
		return this;
	}

	public List<LifecareCalculationIncome> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<LifecareCalculationIncome> incomes) {
		this.incomes = incomes;
	}

	public LifecareCalculation withIncomes(final List<LifecareCalculationIncome> incomes) {
		this.incomes = incomes;
		return this;
	}

	public List<LifecareCalculationExpense> getExpenses() {
		return expenses;
	}

	public void setExpenses(final List<LifecareCalculationExpense> expenses) {
		this.expenses = expenses;
	}

	public LifecareCalculation withExpenses(final List<LifecareCalculationExpense> expenses) {
		this.expenses = expenses;
		return this;
	}

	public List<LifecareCalculationExpense> getSpecialExpenses() {
		return specialExpenses;
	}

	public void setSpecialExpenses(final List<LifecareCalculationExpense> specialExpenses) {
		this.specialExpenses = specialExpenses;
	}

	public LifecareCalculation withSpecialExpenses(final List<LifecareCalculationExpense> specialExpenses) {
		this.specialExpenses = specialExpenses;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareCalculation that = (LifecareCalculation) o;
		return Objects.equals(id, that.id) && Objects.equals(norm, that.norm) && Objects.equals(fromDate, that.fromDate) && Objects.equals(toDate, that.toDate)
			&& Objects.equals(incomeSum, that.incomeSum) && Objects.equals(expenseSum, that.expenseSum) && Objects.equals(specialExpenseSum, that.specialExpenseSum)
			&& Objects.equals(normSum, that.normSum) && Objects.equals(commonHouseholdCost, that.commonHouseholdCost) && Objects.equals(familyCost, that.familyCost)
			&& Objects.equals(balance, that.balance) && Objects.equals(totalSum, that.totalSum) && Objects.equals(isFinal, that.isFinal)
			&& Objects.equals(persons, that.persons) && Objects.equals(incomes, that.incomes) && Objects.equals(expenses, that.expenses)
			&& Objects.equals(specialExpenses, that.specialExpenses);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, norm, fromDate, toDate, incomeSum, expenseSum, specialExpenseSum, normSum, commonHouseholdCost, familyCost, balance, totalSum,
			isFinal, persons, incomes, expenses, specialExpenses);
	}

	@Override
	public String toString() {
		return "LifecareCalculation{id=" + id + ", norm='" + norm + "', fromDate='" + fromDate + "', toDate='" + toDate + "', incomeSum=" + incomeSum
			+ ", expenseSum=" + expenseSum + ", specialExpenseSum=" + specialExpenseSum + ", normSum=" + normSum + ", commonHouseholdCost=" + commonHouseholdCost
			+ ", familyCost=" + familyCost + ", balance=" + balance + ", totalSum=" + totalSum + ", isFinal=" + isFinal + ", persons=" + persons + ", incomes="
			+ incomes + ", expenses=" + expenses + ", specialExpenses=" + specialExpenses + "}";
	}
}
