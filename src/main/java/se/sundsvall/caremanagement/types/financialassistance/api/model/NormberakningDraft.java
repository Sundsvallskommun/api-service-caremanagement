package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * The full draft normberäkning for an errand — header (month + selected norm), the three sections (personer, inkomster,
 * utgifter) and the section sums. personer give the norm base, inkomster are subtracted, utgifter added.
 */
@Schema(description = "The full draft normberäkning — header, the three sections (personer, inkomster, utgifter) and the section sums.")
public class NormberakningDraft {

	@Schema(description = "The errand id", accessMode = Schema.AccessMode.READ_ONLY)
	private String errandId;

	@Schema(description = "The application month (ISO yyyy-MM)", examples = "2026-06")
	private String applicationMonth;

	@Schema(description = "The selected norm id")
	private Integer normId;

	@Schema(description = "The selected norm type")
	private String normType;

	@Schema(description = "The person rows (personer)")
	private List<NormPersonRow> persons = new ArrayList<>();

	@Schema(description = "The income rows (inkomster)")
	private List<NormIncomeRow> incomes = new ArrayList<>();

	@Schema(description = "The expense rows (utgifter)")
	private List<NormExpenseRow> expenses = new ArrayList<>();

	@Schema(description = "The sum of the effective income amounts", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal incomeSum;

	@Schema(description = "The sum of the effective expense amounts", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal expenseSum;

	@Schema(description = "When the draft was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the draft was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static NormberakningDraft create() {
		return new NormberakningDraft();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public NormberakningDraft withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public NormberakningDraft withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public Integer getNormId() {
		return normId;
	}

	public void setNormId(final Integer normId) {
		this.normId = normId;
	}

	public NormberakningDraft withNormId(final Integer normId) {
		this.normId = normId;
		return this;
	}

	public String getNormType() {
		return normType;
	}

	public void setNormType(final String normType) {
		this.normType = normType;
	}

	public NormberakningDraft withNormType(final String normType) {
		this.normType = normType;
		return this;
	}

	public List<NormPersonRow> getPersons() {
		return persons;
	}

	public void setPersons(final List<NormPersonRow> persons) {
		this.persons = persons;
	}

	public NormberakningDraft withPersons(final List<NormPersonRow> persons) {
		this.persons = persons;
		return this;
	}

	public List<NormIncomeRow> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<NormIncomeRow> incomes) {
		this.incomes = incomes;
	}

	public NormberakningDraft withIncomes(final List<NormIncomeRow> incomes) {
		this.incomes = incomes;
		return this;
	}

	public List<NormExpenseRow> getExpenses() {
		return expenses;
	}

	public void setExpenses(final List<NormExpenseRow> expenses) {
		this.expenses = expenses;
	}

	public NormberakningDraft withExpenses(final List<NormExpenseRow> expenses) {
		this.expenses = expenses;
		return this;
	}

	public BigDecimal getIncomeSum() {
		return incomeSum;
	}

	public void setIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
	}

	public NormberakningDraft withIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
		return this;
	}

	public BigDecimal getExpenseSum() {
		return expenseSum;
	}

	public void setExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
	}

	public NormberakningDraft withExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NormberakningDraft withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public NormberakningDraft withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormberakningDraft that = (NormberakningDraft) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(normId, that.normId)
			&& Objects.equals(normType, that.normType) && Objects.equals(persons, that.persons) && Objects.equals(incomes, that.incomes)
			&& Objects.equals(expenses, that.expenses) && Objects.equals(incomeSum, that.incomeSum) && Objects.equals(expenseSum, that.expenseSum)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, normId, normType, persons, incomes, expenses, incomeSum, expenseSum, created, updated);
	}

	@Override
	public String toString() {
		return "NormberakningDraft{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", normId=" + normId +
			", normType='" + normType + '\'' +
			", persons=" + persons +
			", incomes=" + incomes +
			", expenses=" + expenses +
			", incomeSum=" + incomeSum +
			", expenseSum=" + expenseSum +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
