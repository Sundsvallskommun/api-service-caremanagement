package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The Normberäkning tab: careM's draft (source CAREM) until the beräkning is first saved in Lifecare, and the saved
 * beräkning in Lifecare (source LIFECARE) after that, in one shape.
 */
@Schema(description = "The normberäkning, careM's draft or the beräkning saved in Lifecare.")
public class NormberakningDraft {

	@Schema(description = "The errand id (careM draft only)")
	private String errandId;

	@Schema(description = "The application month (yyyy-MM)", examples = "2026-09")
	private String applicationMonth;

	@Schema(description = "The selected norm id", examples = "1")
	private Integer normId;

	@ArraySchema(schema = @Schema(description = "The selected norm types (careM draft only)"))
	private List<String> normType;

	@ArraySchema(schema = @Schema(description = "The labels of the selected norm types, or the Lifecare norm name"))
	private List<String> normTypeDisplayNames;

	@Schema(description = "The start date of the calculation period (ISO date)", examples = "2026-09-01")
	private String calculationFromDate;

	@Schema(description = "The end date of the calculation period (ISO date)", examples = "2026-09-30")
	private String calculationToDate;

	@Schema(description = "The date the calculation is performed (ISO date)", examples = "2026-09-24")
	private String calculationDate;

	@Schema(description = "Whether the household has an own size (Annan hushållsstorlek)")
	private Boolean hasCustomHouseholdSize;

	@Schema(description = "The household size", examples = "3")
	private Integer householdSize;

	@ArraySchema(schema = @Schema(description = "The household members"))
	private List<NormberakningPersonRow> persons;

	@ArraySchema(schema = @Schema(description = "The incomes"))
	private List<NormberakningIncomeRow> incomes;

	@ArraySchema(schema = @Schema(description = "The utgifter"))
	private List<NormberakningExpenseRow> expenses;

	@ArraySchema(schema = @Schema(description = "The levnadskostnader i övrigt"))
	private List<NormberakningExpenseRow> specialExpenses;

	@Schema(description = "The sum of the incomes")
	private BigDecimal incomeSum;

	@Schema(description = "The sum of the utgifter")
	private BigDecimal expenseSum;

	@Schema(description = "The sum of the levnadskostnader i övrigt")
	private BigDecimal specialExpenseSum;

	@Schema(description = "When the draft was created (careM draft only)")
	private String created;

	@Schema(description = "When the draft or the Lifecare beräkning was last saved")
	private String updated;

	@Schema(description = "Where the rows come from: careM's draft, or the beräkning saved in Lifecare (every change is then made there)", allowableValues = {
		"CAREM", "LIFECARE"
	})
	private String source;

	@Schema(description = "Whether Lifecare holds the beräkning as slutlig (Lifecare only)")
	private Boolean finalized;

	@Schema(description = "The gemensamma kostnader of a household of the household size, before the members share is taken (Lifecare only)")
	private BigDecimal amountForHouseholdSize;

	@Schema(description = "The members share of the gemensamma kostnader (Lifecare only)")
	private BigDecimal commonHouseholdCost;

	@Schema(description = "How many members the beräkning includes (Lifecare only)")
	private Integer familyMembers;

	@Schema(description = "Whether the applicant has jobbstimulans in the period (Lifecare only)")
	private Boolean applicantJobStimulus;

	@ArraySchema(schema = @Schema(description = "The norm rows a member can be placed on (Lifecare only)"))
	private List<NormberakningNormRow> normRows;

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

	public List<String> getNormType() {
		return normType;
	}

	public void setNormType(final List<String> normType) {
		this.normType = normType;
	}

	public NormberakningDraft withNormType(final List<String> normType) {
		this.normType = normType;
		return this;
	}

	public List<String> getNormTypeDisplayNames() {
		return normTypeDisplayNames;
	}

	public void setNormTypeDisplayNames(final List<String> normTypeDisplayNames) {
		this.normTypeDisplayNames = normTypeDisplayNames;
	}

	public NormberakningDraft withNormTypeDisplayNames(final List<String> normTypeDisplayNames) {
		this.normTypeDisplayNames = normTypeDisplayNames;
		return this;
	}

	public String getCalculationFromDate() {
		return calculationFromDate;
	}

	public void setCalculationFromDate(final String calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
	}

	public NormberakningDraft withCalculationFromDate(final String calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
		return this;
	}

	public String getCalculationToDate() {
		return calculationToDate;
	}

	public void setCalculationToDate(final String calculationToDate) {
		this.calculationToDate = calculationToDate;
	}

	public NormberakningDraft withCalculationToDate(final String calculationToDate) {
		this.calculationToDate = calculationToDate;
		return this;
	}

	public String getCalculationDate() {
		return calculationDate;
	}

	public void setCalculationDate(final String calculationDate) {
		this.calculationDate = calculationDate;
	}

	public NormberakningDraft withCalculationDate(final String calculationDate) {
		this.calculationDate = calculationDate;
		return this;
	}

	public Boolean getHasCustomHouseholdSize() {
		return hasCustomHouseholdSize;
	}

	public void setHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
	}

	public NormberakningDraft withHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
		return this;
	}

	public Integer getHouseholdSize() {
		return householdSize;
	}

	public void setHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
	}

	public NormberakningDraft withHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
		return this;
	}

	public List<NormberakningPersonRow> getPersons() {
		return persons;
	}

	public void setPersons(final List<NormberakningPersonRow> persons) {
		this.persons = persons;
	}

	public NormberakningDraft withPersons(final List<NormberakningPersonRow> persons) {
		this.persons = persons;
		return this;
	}

	public List<NormberakningIncomeRow> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<NormberakningIncomeRow> incomes) {
		this.incomes = incomes;
	}

	public NormberakningDraft withIncomes(final List<NormberakningIncomeRow> incomes) {
		this.incomes = incomes;
		return this;
	}

	public List<NormberakningExpenseRow> getExpenses() {
		return expenses;
	}

	public void setExpenses(final List<NormberakningExpenseRow> expenses) {
		this.expenses = expenses;
	}

	public NormberakningDraft withExpenses(final List<NormberakningExpenseRow> expenses) {
		this.expenses = expenses;
		return this;
	}

	public List<NormberakningExpenseRow> getSpecialExpenses() {
		return specialExpenses;
	}

	public void setSpecialExpenses(final List<NormberakningExpenseRow> specialExpenses) {
		this.specialExpenses = specialExpenses;
	}

	public NormberakningDraft withSpecialExpenses(final List<NormberakningExpenseRow> specialExpenses) {
		this.specialExpenses = specialExpenses;
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

	public BigDecimal getSpecialExpenseSum() {
		return specialExpenseSum;
	}

	public void setSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
	}

	public NormberakningDraft withSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
		return this;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(final String created) {
		this.created = created;
	}

	public NormberakningDraft withCreated(final String created) {
		this.created = created;
		return this;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(final String updated) {
		this.updated = updated;
	}

	public NormberakningDraft withUpdated(final String updated) {
		this.updated = updated;
		return this;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public NormberakningDraft withSource(final String source) {
		this.source = source;
		return this;
	}

	public Boolean getFinalized() {
		return finalized;
	}

	public void setFinalized(final Boolean finalized) {
		this.finalized = finalized;
	}

	public NormberakningDraft withFinalized(final Boolean finalized) {
		this.finalized = finalized;
		return this;
	}

	public BigDecimal getAmountForHouseholdSize() {
		return amountForHouseholdSize;
	}

	public void setAmountForHouseholdSize(final BigDecimal amountForHouseholdSize) {
		this.amountForHouseholdSize = amountForHouseholdSize;
	}

	public NormberakningDraft withAmountForHouseholdSize(final BigDecimal amountForHouseholdSize) {
		this.amountForHouseholdSize = amountForHouseholdSize;
		return this;
	}

	public BigDecimal getCommonHouseholdCost() {
		return commonHouseholdCost;
	}

	public void setCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
	}

	public NormberakningDraft withCommonHouseholdCost(final BigDecimal commonHouseholdCost) {
		this.commonHouseholdCost = commonHouseholdCost;
		return this;
	}

	public Integer getFamilyMembers() {
		return familyMembers;
	}

	public void setFamilyMembers(final Integer familyMembers) {
		this.familyMembers = familyMembers;
	}

	public NormberakningDraft withFamilyMembers(final Integer familyMembers) {
		this.familyMembers = familyMembers;
		return this;
	}

	public Boolean getApplicantJobStimulus() {
		return applicantJobStimulus;
	}

	public void setApplicantJobStimulus(final Boolean applicantJobStimulus) {
		this.applicantJobStimulus = applicantJobStimulus;
	}

	public NormberakningDraft withApplicantJobStimulus(final Boolean applicantJobStimulus) {
		this.applicantJobStimulus = applicantJobStimulus;
		return this;
	}

	public List<NormberakningNormRow> getNormRows() {
		return normRows;
	}

	public void setNormRows(final List<NormberakningNormRow> normRows) {
		this.normRows = normRows;
	}

	public NormberakningDraft withNormRows(final List<NormberakningNormRow> normRows) {
		this.normRows = normRows;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningDraft that = (NormberakningDraft) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(normId, that.normId) && Objects.equals(normType, that.normType) && Objects.equals(normTypeDisplayNames,
			that.normTypeDisplayNames) && Objects.equals(calculationFromDate, that.calculationFromDate) && Objects.equals(calculationToDate, that.calculationToDate) && Objects.equals(calculationDate, that.calculationDate) && Objects.equals(
				hasCustomHouseholdSize, that.hasCustomHouseholdSize) && Objects.equals(householdSize, that.householdSize) && Objects.equals(persons, that.persons) && Objects.equals(incomes, that.incomes) && Objects.equals(expenses, that.expenses)
			&& Objects.equals(specialExpenses, that.specialExpenses) && Objects.equals(incomeSum, that.incomeSum) && Objects.equals(expenseSum, that.expenseSum) && Objects.equals(specialExpenseSum, that.specialExpenseSum) && Objects.equals(created,
				that.created) && Objects.equals(updated, that.updated) && Objects.equals(source, that.source) && Objects.equals(finalized, that.finalized) && Objects.equals(amountForHouseholdSize, that.amountForHouseholdSize) && Objects.equals(
					commonHouseholdCost, that.commonHouseholdCost) && Objects.equals(familyMembers, that.familyMembers) && Objects.equals(applicantJobStimulus, that.applicantJobStimulus) && Objects.equals(normRows, that.normRows);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, normId, normType, normTypeDisplayNames, calculationFromDate, calculationToDate, calculationDate, hasCustomHouseholdSize, householdSize, persons, incomes, expenses, specialExpenses, incomeSum,
			expenseSum, specialExpenseSum, created, updated, source, finalized, amountForHouseholdSize, commonHouseholdCost, familyMembers, applicantJobStimulus, normRows);
	}

	@Override
	public String toString() {
		return "NormberakningDraft{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", normId=" + normId +
			", normType=" + normType +
			", normTypeDisplayNames=" + normTypeDisplayNames +
			", calculationFromDate='" + calculationFromDate + '\'' +
			", calculationToDate='" + calculationToDate + '\'' +
			", calculationDate='" + calculationDate + '\'' +
			", hasCustomHouseholdSize=" + hasCustomHouseholdSize +
			", householdSize=" + householdSize +
			", persons=" + persons +
			", incomes=" + incomes +
			", expenses=" + expenses +
			", specialExpenses=" + specialExpenses +
			", incomeSum=" + incomeSum +
			", expenseSum=" + expenseSum +
			", specialExpenseSum=" + specialExpenseSum +
			", created='" + created + '\'' +
			", updated='" + updated + '\'' +
			", source='" + source + '\'' +
			", finalized=" + finalized +
			", amountForHouseholdSize=" + amountForHouseholdSize +
			", commonHouseholdCost=" + commonHouseholdCost +
			", familyMembers=" + familyMembers +
			", applicantJobStimulus=" + applicantJobStimulus +
			", normRows=" + normRows +
			'}';
	}
}
