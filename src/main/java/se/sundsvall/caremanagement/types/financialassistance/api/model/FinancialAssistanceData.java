package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "The typed financial assistance application payload.")
public class FinancialAssistanceData {

	@Schema(description = "The type of application", examples = "NEW", allowableValues = {
		"NEW", "RENEWAL", "SUPPLEMENTARY"
	})
	@OneOf(value = {
		"NEW", "RENEWAL", "SUPPLEMENTARY"
	}, nullable = true)
	private String applicationType;

	@Schema(description = "Marital status of the applicant", examples = "SINGLE", allowableValues = {
		"SINGLE", "COHABITING"
	})
	@OneOf(value = {
		"SINGLE", "COHABITING"
	}, nullable = true)
	private String maritalStatus;

	@Schema(description = "The month the application period concerns", examples = "6")
	private Integer periodMonth;

	@Schema(description = "The year the application period concerns", examples = "2026")
	private Integer periodYear;

	@Schema(description = "Choice of application period", examples = "CURRENT_MONTH", allowableValues = {
		"CURRENT_MONTH", "NEXT_MONTH", "OTHER_BENEFIT"
	})
	@OneOf(value = {
		"CURRENT_MONTH", "NEXT_MONTH", "OTHER_BENEFIT"
	}, nullable = true)
	private String periodChoice;

	@Schema(description = "The norm type used for the calculation", examples = "NATIONAL_NORM", allowableValues = {
		"NATIONAL_NORM", "OTHER_NORM"
	})
	@OneOf(value = {
		"NATIONAL_NORM", "OTHER_NORM"
	}, nullable = true)
	private String normType;

	@Schema(description = "Description of the other benefit", examples = "Establishment benefit")
	private String otherBenefitDescription;

	@Schema(description = "Description of the applicant's livelihood", examples = "Job-seeking via the employment agency")
	private String livelihoodDescription;

	@Schema(description = "Whether the household has children under 21", examples = "true")
	private Boolean hasChildrenUnder21;

	@Schema(description = "Whether the children's residence situation has changed", examples = "false")
	private Boolean childrenResidenceChanged;

	@Schema(description = "Description of the change in children's residence", examples = "The children now live in shared custody")
	private String childrenResidenceChangeDescription;

	@Schema(description = "The household's housing form", examples = "RENTAL", allowableValues = {
		"NO_HOUSING_OR_INSTITUTION", "RENTAL", "SUBLET", "LODGER", "CONDOMINIUM", "OWNED_HOUSE", "RENTED_HOUSE", "LIVING_WITH_PARENTS"
	})
	@OneOf(value = {
		"NO_HOUSING_OR_INSTITUTION", "RENTAL", "SUBLET", "LODGER", "CONDOMINIUM", "OWNED_HOUSE", "RENTED_HOUSE", "LIVING_WITH_PARENTS"
	}, nullable = true)
	private String housingForm;

	@Schema(description = "Total number of persons (adults and children) living in the housing", examples = "3")
	private Integer housingPersonCount;

	@Schema(description = "Number of rooms plus kitchen", examples = "3")
	private Integer housingRoomsPlusKitchen;

	@Schema(description = "Free text description of the housing", examples = "Three-room rental apartment")
	private String housingDescription;

	@Schema(description = "Whether the housing situation has changed", examples = "false")
	private Boolean housingChanged;

	@Schema(description = "Description of the housing change", examples = "Moved to a smaller apartment in May")
	private String housingChangeDescription;

	@Schema(description = "Whether the household has incomes", examples = "true")
	private Boolean hasIncomes;

	@Schema(description = "Whether the household has pending benefits", examples = "false")
	private Boolean hasPendingBenefits;

	@Schema(description = "Whether the household has assets", examples = "false")
	private Boolean hasAssets;

	@Schema(description = "Whether the applicant stays in the municipality", examples = "true")
	private Boolean staysInMunicipality;

	@Schema(description = "Description of the applicant's stay", examples = "Lives at the registered address")
	private String stayDescription;

	@Schema(description = "Whether the applicant has attested the application", examples = "true")
	private Boolean attestation;

	@Schema(description = "When the application was attested", examples = "2026-06-01T09:30:00Z")
	private OffsetDateTime attestedAt;

	@Schema(description = "Children included in the application")
	@Valid
	private List<Child> children;

	@Schema(description = "Costs applied for")
	@Valid
	private List<Cost> costs;

	@Schema(description = "Incomes reported")
	@Valid
	private List<Income> incomes;

	@Schema(description = "Pending benefits")
	@Valid
	private List<PendingBenefit> pendingBenefits;

	@Schema(description = "Assets owned")
	@Valid
	private List<Asset> assets;

	@Schema(description = "Persons on the application")
	@Valid
	private List<Person> persons;

	@Schema(description = "Plannings towards self-sufficiency")
	@Valid
	private List<Planning> plannings;

	@Schema(description = "Planned activities")
	@Valid
	private List<PlannedActivity> plannedActivities;

	@Schema(description = "Job applications")
	@Valid
	private List<JobApplication> jobApplications;

	public static FinancialAssistanceData create() {
		return new FinancialAssistanceData();
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(final String applicationType) {
		this.applicationType = applicationType;
	}

	public FinancialAssistanceData withApplicationType(final String applicationType) {
		this.applicationType = applicationType;
		return this;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(final String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public FinancialAssistanceData withMaritalStatus(final String maritalStatus) {
		this.maritalStatus = maritalStatus;
		return this;
	}

	public Integer getPeriodMonth() {
		return periodMonth;
	}

	public void setPeriodMonth(final Integer periodMonth) {
		this.periodMonth = periodMonth;
	}

	public FinancialAssistanceData withPeriodMonth(final Integer periodMonth) {
		this.periodMonth = periodMonth;
		return this;
	}

	public Integer getPeriodYear() {
		return periodYear;
	}

	public void setPeriodYear(final Integer periodYear) {
		this.periodYear = periodYear;
	}

	public FinancialAssistanceData withPeriodYear(final Integer periodYear) {
		this.periodYear = periodYear;
		return this;
	}

	public String getPeriodChoice() {
		return periodChoice;
	}

	public void setPeriodChoice(final String periodChoice) {
		this.periodChoice = periodChoice;
	}

	public FinancialAssistanceData withPeriodChoice(final String periodChoice) {
		this.periodChoice = periodChoice;
		return this;
	}

	public String getNormType() {
		return normType;
	}

	public void setNormType(final String normType) {
		this.normType = normType;
	}

	public FinancialAssistanceData withNormType(final String normType) {
		this.normType = normType;
		return this;
	}

	public String getOtherBenefitDescription() {
		return otherBenefitDescription;
	}

	public void setOtherBenefitDescription(final String otherBenefitDescription) {
		this.otherBenefitDescription = otherBenefitDescription;
	}

	public FinancialAssistanceData withOtherBenefitDescription(final String otherBenefitDescription) {
		this.otherBenefitDescription = otherBenefitDescription;
		return this;
	}

	public String getLivelihoodDescription() {
		return livelihoodDescription;
	}

	public void setLivelihoodDescription(final String livelihoodDescription) {
		this.livelihoodDescription = livelihoodDescription;
	}

	public FinancialAssistanceData withLivelihoodDescription(final String livelihoodDescription) {
		this.livelihoodDescription = livelihoodDescription;
		return this;
	}

	public Boolean getHasChildrenUnder21() {
		return hasChildrenUnder21;
	}

	public void setHasChildrenUnder21(final Boolean hasChildrenUnder21) {
		this.hasChildrenUnder21 = hasChildrenUnder21;
	}

	public FinancialAssistanceData withHasChildrenUnder21(final Boolean hasChildrenUnder21) {
		this.hasChildrenUnder21 = hasChildrenUnder21;
		return this;
	}

	public Boolean getChildrenResidenceChanged() {
		return childrenResidenceChanged;
	}

	public void setChildrenResidenceChanged(final Boolean childrenResidenceChanged) {
		this.childrenResidenceChanged = childrenResidenceChanged;
	}

	public FinancialAssistanceData withChildrenResidenceChanged(final Boolean childrenResidenceChanged) {
		this.childrenResidenceChanged = childrenResidenceChanged;
		return this;
	}

	public String getChildrenResidenceChangeDescription() {
		return childrenResidenceChangeDescription;
	}

	public void setChildrenResidenceChangeDescription(final String childrenResidenceChangeDescription) {
		this.childrenResidenceChangeDescription = childrenResidenceChangeDescription;
	}

	public FinancialAssistanceData withChildrenResidenceChangeDescription(final String childrenResidenceChangeDescription) {
		this.childrenResidenceChangeDescription = childrenResidenceChangeDescription;
		return this;
	}

	public String getHousingForm() {
		return housingForm;
	}

	public void setHousingForm(final String housingForm) {
		this.housingForm = housingForm;
	}

	public FinancialAssistanceData withHousingForm(final String housingForm) {
		this.housingForm = housingForm;
		return this;
	}

	public Integer getHousingPersonCount() {
		return housingPersonCount;
	}

	public void setHousingPersonCount(final Integer housingPersonCount) {
		this.housingPersonCount = housingPersonCount;
	}

	public FinancialAssistanceData withHousingPersonCount(final Integer housingPersonCount) {
		this.housingPersonCount = housingPersonCount;
		return this;
	}

	public Integer getHousingRoomsPlusKitchen() {
		return housingRoomsPlusKitchen;
	}

	public void setHousingRoomsPlusKitchen(final Integer housingRoomsPlusKitchen) {
		this.housingRoomsPlusKitchen = housingRoomsPlusKitchen;
	}

	public FinancialAssistanceData withHousingRoomsPlusKitchen(final Integer housingRoomsPlusKitchen) {
		this.housingRoomsPlusKitchen = housingRoomsPlusKitchen;
		return this;
	}

	public String getHousingDescription() {
		return housingDescription;
	}

	public void setHousingDescription(final String housingDescription) {
		this.housingDescription = housingDescription;
	}

	public FinancialAssistanceData withHousingDescription(final String housingDescription) {
		this.housingDescription = housingDescription;
		return this;
	}

	public Boolean getHousingChanged() {
		return housingChanged;
	}

	public void setHousingChanged(final Boolean housingChanged) {
		this.housingChanged = housingChanged;
	}

	public FinancialAssistanceData withHousingChanged(final Boolean housingChanged) {
		this.housingChanged = housingChanged;
		return this;
	}

	public String getHousingChangeDescription() {
		return housingChangeDescription;
	}

	public void setHousingChangeDescription(final String housingChangeDescription) {
		this.housingChangeDescription = housingChangeDescription;
	}

	public FinancialAssistanceData withHousingChangeDescription(final String housingChangeDescription) {
		this.housingChangeDescription = housingChangeDescription;
		return this;
	}

	public Boolean getHasIncomes() {
		return hasIncomes;
	}

	public void setHasIncomes(final Boolean hasIncomes) {
		this.hasIncomes = hasIncomes;
	}

	public FinancialAssistanceData withHasIncomes(final Boolean hasIncomes) {
		this.hasIncomes = hasIncomes;
		return this;
	}

	public Boolean getHasPendingBenefits() {
		return hasPendingBenefits;
	}

	public void setHasPendingBenefits(final Boolean hasPendingBenefits) {
		this.hasPendingBenefits = hasPendingBenefits;
	}

	public FinancialAssistanceData withHasPendingBenefits(final Boolean hasPendingBenefits) {
		this.hasPendingBenefits = hasPendingBenefits;
		return this;
	}

	public Boolean getHasAssets() {
		return hasAssets;
	}

	public void setHasAssets(final Boolean hasAssets) {
		this.hasAssets = hasAssets;
	}

	public FinancialAssistanceData withHasAssets(final Boolean hasAssets) {
		this.hasAssets = hasAssets;
		return this;
	}

	public Boolean getStaysInMunicipality() {
		return staysInMunicipality;
	}

	public void setStaysInMunicipality(final Boolean staysInMunicipality) {
		this.staysInMunicipality = staysInMunicipality;
	}

	public FinancialAssistanceData withStaysInMunicipality(final Boolean staysInMunicipality) {
		this.staysInMunicipality = staysInMunicipality;
		return this;
	}

	public String getStayDescription() {
		return stayDescription;
	}

	public void setStayDescription(final String stayDescription) {
		this.stayDescription = stayDescription;
	}

	public FinancialAssistanceData withStayDescription(final String stayDescription) {
		this.stayDescription = stayDescription;
		return this;
	}

	public Boolean getAttestation() {
		return attestation;
	}

	public void setAttestation(final Boolean attestation) {
		this.attestation = attestation;
	}

	public FinancialAssistanceData withAttestation(final Boolean attestation) {
		this.attestation = attestation;
		return this;
	}

	public OffsetDateTime getAttestedAt() {
		return attestedAt;
	}

	public void setAttestedAt(final OffsetDateTime attestedAt) {
		this.attestedAt = attestedAt;
	}

	public FinancialAssistanceData withAttestedAt(final OffsetDateTime attestedAt) {
		this.attestedAt = attestedAt;
		return this;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(final List<Child> children) {
		this.children = children;
	}

	public FinancialAssistanceData withChildren(final List<Child> children) {
		this.children = children;
		return this;
	}

	public List<Cost> getCosts() {
		return costs;
	}

	public void setCosts(final List<Cost> costs) {
		this.costs = costs;
	}

	public FinancialAssistanceData withCosts(final List<Cost> costs) {
		this.costs = costs;
		return this;
	}

	public List<Income> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<Income> incomes) {
		this.incomes = incomes;
	}

	public FinancialAssistanceData withIncomes(final List<Income> incomes) {
		this.incomes = incomes;
		return this;
	}

	public List<PendingBenefit> getPendingBenefits() {
		return pendingBenefits;
	}

	public void setPendingBenefits(final List<PendingBenefit> pendingBenefits) {
		this.pendingBenefits = pendingBenefits;
	}

	public FinancialAssistanceData withPendingBenefits(final List<PendingBenefit> pendingBenefits) {
		this.pendingBenefits = pendingBenefits;
		return this;
	}

	public List<Asset> getAssets() {
		return assets;
	}

	public void setAssets(final List<Asset> assets) {
		this.assets = assets;
	}

	public FinancialAssistanceData withAssets(final List<Asset> assets) {
		this.assets = assets;
		return this;
	}

	public List<Person> getPersons() {
		return persons;
	}

	public void setPersons(final List<Person> persons) {
		this.persons = persons;
	}

	public FinancialAssistanceData withPersons(final List<Person> persons) {
		this.persons = persons;
		return this;
	}

	public List<Planning> getPlannings() {
		return plannings;
	}

	public void setPlannings(final List<Planning> plannings) {
		this.plannings = plannings;
	}

	public FinancialAssistanceData withPlannings(final List<Planning> plannings) {
		this.plannings = plannings;
		return this;
	}

	public List<PlannedActivity> getPlannedActivities() {
		return plannedActivities;
	}

	public void setPlannedActivities(final List<PlannedActivity> plannedActivities) {
		this.plannedActivities = plannedActivities;
	}

	public FinancialAssistanceData withPlannedActivities(final List<PlannedActivity> plannedActivities) {
		this.plannedActivities = plannedActivities;
		return this;
	}

	public List<JobApplication> getJobApplications() {
		return jobApplications;
	}

	public void setJobApplications(final List<JobApplication> jobApplications) {
		this.jobApplications = jobApplications;
	}

	public FinancialAssistanceData withJobApplications(final List<JobApplication> jobApplications) {
		this.jobApplications = jobApplications;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinancialAssistanceData that = (FinancialAssistanceData) o;
		return Objects.equals(applicationType, that.applicationType) && Objects.equals(maritalStatus, that.maritalStatus)
			&& Objects.equals(periodMonth, that.periodMonth) && Objects.equals(periodYear, that.periodYear)
			&& Objects.equals(periodChoice, that.periodChoice) && Objects.equals(normType, that.normType)
			&& Objects.equals(otherBenefitDescription, that.otherBenefitDescription)
			&& Objects.equals(livelihoodDescription, that.livelihoodDescription)
			&& Objects.equals(hasChildrenUnder21, that.hasChildrenUnder21)
			&& Objects.equals(childrenResidenceChanged, that.childrenResidenceChanged)
			&& Objects.equals(childrenResidenceChangeDescription, that.childrenResidenceChangeDescription)
			&& Objects.equals(housingForm, that.housingForm) && Objects.equals(housingPersonCount, that.housingPersonCount)
			&& Objects.equals(housingRoomsPlusKitchen, that.housingRoomsPlusKitchen)
			&& Objects.equals(housingDescription, that.housingDescription) && Objects.equals(housingChanged, that.housingChanged)
			&& Objects.equals(housingChangeDescription, that.housingChangeDescription) && Objects.equals(hasIncomes, that.hasIncomes)
			&& Objects.equals(hasPendingBenefits, that.hasPendingBenefits) && Objects.equals(hasAssets, that.hasAssets)
			&& Objects.equals(staysInMunicipality, that.staysInMunicipality) && Objects.equals(stayDescription, that.stayDescription)
			&& Objects.equals(attestation, that.attestation) && Objects.equals(attestedAt, that.attestedAt)
			&& Objects.equals(children, that.children) && Objects.equals(costs, that.costs) && Objects.equals(incomes, that.incomes)
			&& Objects.equals(pendingBenefits, that.pendingBenefits) && Objects.equals(assets, that.assets)
			&& Objects.equals(persons, that.persons) && Objects.equals(plannings, that.plannings)
			&& Objects.equals(plannedActivities, that.plannedActivities) && Objects.equals(jobApplications, that.jobApplications);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicationType, maritalStatus, periodMonth, periodYear, periodChoice, normType,
			otherBenefitDescription, livelihoodDescription, hasChildrenUnder21, childrenResidenceChanged,
			childrenResidenceChangeDescription, housingForm, housingPersonCount, housingRoomsPlusKitchen,
			housingDescription, housingChanged, housingChangeDescription, hasIncomes, hasPendingBenefits, hasAssets,
			staysInMunicipality, stayDescription, attestation, attestedAt, children, costs, incomes, pendingBenefits, assets,
			persons, plannings, plannedActivities, jobApplications);
	}

	@Override
	public String toString() {
		return "FinancialAssistanceData{applicationType='" + applicationType + "', maritalStatus='" + maritalStatus
			+ "', periodMonth=" + periodMonth + ", periodYear=" + periodYear + ", periodChoice='" + periodChoice
			+ "', normType='" + normType + "', otherBenefitDescription='" + otherBenefitDescription
			+ "', livelihoodDescription='" + livelihoodDescription + "', hasChildrenUnder21=" + hasChildrenUnder21
			+ ", childrenResidenceChanged=" + childrenResidenceChanged + ", childrenResidenceChangeDescription='"
			+ childrenResidenceChangeDescription + "', housingForm='" + housingForm + "', housingPersonCount="
			+ housingPersonCount + ", housingRoomsPlusKitchen="
			+ housingRoomsPlusKitchen + ", housingDescription='" + housingDescription + "', housingChanged=" + housingChanged
			+ ", housingChangeDescription='" + housingChangeDescription + "', hasIncomes=" + hasIncomes + ", hasPendingBenefits="
			+ hasPendingBenefits + ", hasAssets=" + hasAssets + ", staysInMunicipality=" + staysInMunicipality
			+ ", stayDescription='" + stayDescription + "', attestation=" + attestation + ", attestedAt=" + attestedAt
			+ ", children=" + children + ", costs=" + costs + ", incomes=" + incomes + ", pendingBenefits=" + pendingBenefits
			+ ", assets=" + assets + ", persons=" + persons + ", plannings=" + plannings + ", plannedActivities="
			+ plannedActivities + ", jobApplications=" + jobApplications + '}';
	}
}
