package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The beräkning preceding the errand's own period, read from Lifecare. Read-only; the sums are Lifecare's own and
 * positive, only balance keeps its sign.
 */
@Schema(description = "The beräkning preceding the errand's own period, read from Lifecare.")
public class NormberakningPreviousCalculation {

	@Schema(description = "Lifecare's calculation id", examples = "1")
	private Integer id;

	@Schema(description = "The norm", examples = "Riksnorm 2026")
	private String norm;

	@Schema(description = "The start of the period", examples = "2026-01-01")
	private String fromDate;

	@Schema(description = "The end of the period", examples = "2026-06-30")
	private String toDate;

	@Schema(description = "The sum of the incomes")
	private BigDecimal incomeSum;

	@Schema(description = "The sum of the utgifter")
	private BigDecimal expenseSum;

	@Schema(description = "The sum of the levnadskostnader i övrigt")
	private BigDecimal specialExpenseSum;

	@Schema(description = "The sum of the norm")
	private BigDecimal normSum;

	@Schema(description = "The gemensamma kostnader")
	private BigDecimal commonHouseholdCost;

	@Schema(description = "The norm part for the members")
	private BigDecimal familyCost;

	@Schema(description = "The result, an underskott negative")
	private BigDecimal balance;

	@Schema(description = "The total sum")
	private BigDecimal totalSum;

	@Schema(description = "Whether the beräkning is slutlig")
	private Boolean isFinal;

	@ArraySchema(schema = @Schema(description = "The members included"))
	private List<NormberakningPreviousPerson> persons;

	@ArraySchema(schema = @Schema(description = "The incomes"))
	private List<NormberakningPreviousIncome> incomes;

	@ArraySchema(schema = @Schema(description = "The utgifter"))
	private List<NormberakningPreviousExpense> expenses;

	@ArraySchema(schema = @Schema(description = "The levnadskostnader i övrigt"))
	private List<NormberakningPreviousExpense> specialExpenses;

	public static NormberakningPreviousCalculation create() {
		return new NormberakningPreviousCalculation();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public NormberakningPreviousCalculation withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getNorm() {
		return norm;
	}

	public void setNorm(final String norm) {
		this.norm = norm;
	}

	public NormberakningPreviousCalculation withNorm(final String norm) {
		this.norm = norm;
		return this;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(final String fromDate) {
		this.fromDate = fromDate;
	}

	public NormberakningPreviousCalculation withFromDate(final String fromDate) {
		this.fromDate = fromDate;
		return this;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(final String toDate) {
		this.toDate = toDate;
	}

	public NormberakningPreviousCalculation withToDate(final String toDate) {
		this.toDate = toDate;
		return this;
	}

	public BigDecimal getIncomeSum() {
		return incomeSum;
	}

	public void setIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
	}

	public NormberakningPreviousCalculation withIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
		return this;
	}

	public BigDecimal getExpenseSum() {
		return expenseSum;
	}

	public void setExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
	}

	public NormberakningPreviousCalculation withExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
		return this;
	}

	public BigDecimal getSpecialExpenseSum() {
		return specialExpenseSum;
	}

	public void setSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
	}

	public NormberakningPreviousCalculation withSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
		return this;
	}

	public BigDecimal getNormSum() {
		return normSum;
	}

	public void setNormSum(final BigDecimal normSum) {
		this.normSum = normSum;
	}

	public NormberakningPreviousCalculation withNormSum(final BigDecimal normSum) {
		this.normSum = normSum;
		return this;
	}

	public BigDecimal getCommonHouseholdCost() {
		return commonHouseholdCost;
	}

	public void setCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
	}

	public NormberakningPreviousCalculation withCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
		return this;
	}

	public BigDecimal getFamilyCost() {
		return familyCost;
	}

	public void setFamilyCost(final BigDecimal familyCost) {
		this.familyCost = familyCost;
	}

	public NormberakningPreviousCalculation withFamilyCost(final BigDecimal familyCost) {
		this.familyCost = familyCost;
		return this;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(final BigDecimal balance) {
		this.balance = balance;
	}

	public NormberakningPreviousCalculation withBalance(final BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public BigDecimal getTotalSum() {
		return totalSum;
	}

	public void setTotalSum(final BigDecimal totalSum) {
		this.totalSum = totalSum;
	}

	public NormberakningPreviousCalculation withTotalSum(final BigDecimal totalSum) {
		this.totalSum = totalSum;
		return this;
	}

	public Boolean getIsFinal() {
		return isFinal;
	}

	public void setIsFinal(final Boolean isFinal) {
		this.isFinal = isFinal;
	}

	public NormberakningPreviousCalculation withIsFinal(final Boolean isFinal) {
		this.isFinal = isFinal;
		return this;
	}

	public List<NormberakningPreviousPerson> getPersons() {
		return persons;
	}

	public void setPersons(final List<NormberakningPreviousPerson> persons) {
		this.persons = persons;
	}

	public NormberakningPreviousCalculation withPersons(final List<NormberakningPreviousPerson> persons) {
		this.persons = persons;
		return this;
	}

	public List<NormberakningPreviousIncome> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<NormberakningPreviousIncome> incomes) {
		this.incomes = incomes;
	}

	public NormberakningPreviousCalculation withIncomes(final List<NormberakningPreviousIncome> incomes) {
		this.incomes = incomes;
		return this;
	}

	public List<NormberakningPreviousExpense> getExpenses() {
		return expenses;
	}

	public void setExpenses(final List<NormberakningPreviousExpense> expenses) {
		this.expenses = expenses;
	}

	public NormberakningPreviousCalculation withExpenses(final List<NormberakningPreviousExpense> expenses) {
		this.expenses = expenses;
		return this;
	}

	public List<NormberakningPreviousExpense> getSpecialExpenses() {
		return specialExpenses;
	}

	public void setSpecialExpenses(final List<NormberakningPreviousExpense> specialExpenses) {
		this.specialExpenses = specialExpenses;
	}

	public NormberakningPreviousCalculation withSpecialExpenses(final List<NormberakningPreviousExpense> specialExpenses) {
		this.specialExpenses = specialExpenses;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningPreviousCalculation that = (NormberakningPreviousCalculation) o;
		return Objects.equals(id, that.id) && Objects.equals(norm, that.norm) && Objects.equals(fromDate, that.fromDate) && Objects.equals(toDate, that.toDate) && Objects.equals(incomeSum, that.incomeSum) && Objects.equals(expenseSum, that.expenseSum)
			&& Objects.equals(specialExpenseSum, that.specialExpenseSum) && Objects.equals(normSum, that.normSum) && Objects.equals(commonHouseholdCost, that.commonHouseholdCost) && Objects.equals(familyCost, that.familyCost) && Objects.equals(balance,
				that.balance) && Objects.equals(totalSum, that.totalSum) && Objects.equals(isFinal, that.isFinal) && Objects.equals(persons, that.persons) && Objects.equals(incomes, that.incomes) && Objects.equals(expenses, that.expenses) && Objects
					.equals(specialExpenses, that.specialExpenses);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, norm, fromDate, toDate, incomeSum, expenseSum, specialExpenseSum, normSum, commonHouseholdCost, familyCost, balance, totalSum, isFinal, persons, incomes, expenses, specialExpenses);
	}

	@Override
	public String toString() {
		return "NormberakningPreviousCalculation{" +
			"id=" + id +
			", norm='" + norm + '\'' +
			", fromDate='" + fromDate + '\'' +
			", toDate='" + toDate + '\'' +
			", incomeSum=" + incomeSum +
			", expenseSum=" + expenseSum +
			", specialExpenseSum=" + specialExpenseSum +
			", normSum=" + normSum +
			", commonHouseholdCost=" + commonHouseholdCost +
			", familyCost=" + familyCost +
			", balance=" + balance +
			", totalSum=" + totalSum +
			", isFinal=" + isFinal +
			", persons=" + persons +
			", incomes=" + incomes +
			", expenses=" + expenses +
			", specialExpenses=" + specialExpenses +
			'}';
	}
}
