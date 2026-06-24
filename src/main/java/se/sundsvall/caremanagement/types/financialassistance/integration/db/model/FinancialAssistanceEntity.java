package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

import static org.hibernate.Length.LONG32;

/**
 * Financial assistance (EB) application data. One row per errand, sharing the primary key with {@code errand.id}
 * ({@code errand_id} is set from the envelope id, not generated). Repeating groups — children, costs, incomes, pending
 * benefits, assets, per-person details, planning, planned activities and job applications — are owned
 * {@code @ElementCollection} value tables ({@code errand_fa_*}) that cascade with the row.
 */
@Entity
@Table(name = "errand_financial_assistance")
@EntityListeners(AuditableListener.class)
public class FinancialAssistanceEntity implements Auditable {

	@Id
	@Column(name = "errand_id", length = 255)
	private String errandId;

	@Column(name = "application_type", length = 32)
	private String applicationType;

	@Column(name = "marital_status", length = 32)
	private String maritalStatus;

	@Column(name = "period_month")
	private Integer periodMonth;

	@Column(name = "period_year")
	private Integer periodYear;

	@Column(name = "period_choice", length = 32)
	private String periodChoice;

	@Column(name = "norm_type", length = 32)
	private String normType;

	@Column(name = "other_benefit_description", length = LONG32)
	private String otherBenefitDescription;

	@Column(name = "livelihood_description", length = LONG32)
	private String livelihoodDescription;

	@Column(name = "has_children_under21")
	private Boolean hasChildrenUnder21;

	@Column(name = "children_residence_changed")
	private Boolean childrenResidenceChanged;

	@Column(name = "children_residence_change_description", length = LONG32)
	private String childrenResidenceChangeDescription;

	@Column(name = "housing_form", length = 32)
	private String housingForm;

	@Column(name = "housing_person_count")
	private Integer housingPersonCount;

	@Column(name = "housing_rooms_plus_kitchen")
	private Integer housingRoomsPlusKitchen;

	@Column(name = "housing_description", length = LONG32)
	private String housingDescription;

	@Column(name = "housing_changed")
	private Boolean housingChanged;

	@Column(name = "housing_change_description", length = LONG32)
	private String housingChangeDescription;

	@Column(name = "has_incomes")
	private Boolean hasIncomes;

	@Column(name = "has_pending_benefits")
	private Boolean hasPendingBenefits;

	@Column(name = "has_assets")
	private Boolean hasAssets;

	@Column(name = "stays_in_municipality")
	private Boolean staysInMunicipality;

	@Column(name = "stay_description", length = LONG32)
	private String stayDescription;

	@Column(name = "attestation")
	private Boolean attestation;

	@Column(name = "attested_at")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime attestedAt;

	@Column(name = "last_daily_run_at")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime lastDailyRunAt;

	@ElementCollection
	@CollectionTable(name = "errand_fa_child", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaChild> children;

	@ElementCollection
	@CollectionTable(name = "errand_fa_cost", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaCost> costs;

	@ElementCollection
	@CollectionTable(name = "errand_fa_income", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaIncome> incomes;

	@ElementCollection
	@CollectionTable(name = "errand_fa_pending_benefit", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaPendingBenefit> pendingBenefits;

	@ElementCollection
	@CollectionTable(name = "errand_fa_asset", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaAsset> assets;

	@ElementCollection
	@CollectionTable(name = "errand_fa_person", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaPerson> persons;

	@ElementCollection
	@CollectionTable(name = "errand_fa_planning", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaPlanning> plannings;

	@ElementCollection
	@CollectionTable(name = "errand_fa_planned_activity", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaPlannedActivity> plannedActivities;

	@ElementCollection
	@CollectionTable(name = "errand_fa_job_application", joinColumns = @JoinColumn(name = "errand_id"))
	private List<FaJobApplication> jobApplications;

	@Column(name = "created")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime modified;

	public static FinancialAssistanceEntity create() {
		return new FinancialAssistanceEntity();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(final String applicationType) {
		this.applicationType = applicationType;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(final String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public Integer getPeriodMonth() {
		return periodMonth;
	}

	public void setPeriodMonth(final Integer periodMonth) {
		this.periodMonth = periodMonth;
	}

	public Integer getPeriodYear() {
		return periodYear;
	}

	public void setPeriodYear(final Integer periodYear) {
		this.periodYear = periodYear;
	}

	public String getPeriodChoice() {
		return periodChoice;
	}

	public void setPeriodChoice(final String periodChoice) {
		this.periodChoice = periodChoice;
	}

	public String getNormType() {
		return normType;
	}

	public void setNormType(final String normType) {
		this.normType = normType;
	}

	public String getOtherBenefitDescription() {
		return otherBenefitDescription;
	}

	public void setOtherBenefitDescription(final String otherBenefitDescription) {
		this.otherBenefitDescription = otherBenefitDescription;
	}

	public String getLivelihoodDescription() {
		return livelihoodDescription;
	}

	public void setLivelihoodDescription(final String livelihoodDescription) {
		this.livelihoodDescription = livelihoodDescription;
	}

	public Boolean getHasChildrenUnder21() {
		return hasChildrenUnder21;
	}

	public void setHasChildrenUnder21(final Boolean hasChildrenUnder21) {
		this.hasChildrenUnder21 = hasChildrenUnder21;
	}

	public Boolean getChildrenResidenceChanged() {
		return childrenResidenceChanged;
	}

	public void setChildrenResidenceChanged(final Boolean childrenResidenceChanged) {
		this.childrenResidenceChanged = childrenResidenceChanged;
	}

	public String getChildrenResidenceChangeDescription() {
		return childrenResidenceChangeDescription;
	}

	public void setChildrenResidenceChangeDescription(final String childrenResidenceChangeDescription) {
		this.childrenResidenceChangeDescription = childrenResidenceChangeDescription;
	}

	public String getHousingForm() {
		return housingForm;
	}

	public void setHousingForm(final String housingForm) {
		this.housingForm = housingForm;
	}

	public Integer getHousingPersonCount() {
		return housingPersonCount;
	}

	public void setHousingPersonCount(final Integer housingPersonCount) {
		this.housingPersonCount = housingPersonCount;
	}

	public Integer getHousingRoomsPlusKitchen() {
		return housingRoomsPlusKitchen;
	}

	public void setHousingRoomsPlusKitchen(final Integer housingRoomsPlusKitchen) {
		this.housingRoomsPlusKitchen = housingRoomsPlusKitchen;
	}

	public String getHousingDescription() {
		return housingDescription;
	}

	public void setHousingDescription(final String housingDescription) {
		this.housingDescription = housingDescription;
	}

	public Boolean getHousingChanged() {
		return housingChanged;
	}

	public void setHousingChanged(final Boolean housingChanged) {
		this.housingChanged = housingChanged;
	}

	public String getHousingChangeDescription() {
		return housingChangeDescription;
	}

	public void setHousingChangeDescription(final String housingChangeDescription) {
		this.housingChangeDescription = housingChangeDescription;
	}

	public Boolean getHasIncomes() {
		return hasIncomes;
	}

	public void setHasIncomes(final Boolean hasIncomes) {
		this.hasIncomes = hasIncomes;
	}

	public Boolean getHasPendingBenefits() {
		return hasPendingBenefits;
	}

	public void setHasPendingBenefits(final Boolean hasPendingBenefits) {
		this.hasPendingBenefits = hasPendingBenefits;
	}

	public Boolean getHasAssets() {
		return hasAssets;
	}

	public void setHasAssets(final Boolean hasAssets) {
		this.hasAssets = hasAssets;
	}

	public Boolean getStaysInMunicipality() {
		return staysInMunicipality;
	}

	public void setStaysInMunicipality(final Boolean staysInMunicipality) {
		this.staysInMunicipality = staysInMunicipality;
	}

	public String getStayDescription() {
		return stayDescription;
	}

	public void setStayDescription(final String stayDescription) {
		this.stayDescription = stayDescription;
	}

	public Boolean getAttestation() {
		return attestation;
	}

	public void setAttestation(final Boolean attestation) {
		this.attestation = attestation;
	}

	public OffsetDateTime getAttestedAt() {
		return attestedAt;
	}

	public void setAttestedAt(final OffsetDateTime attestedAt) {
		this.attestedAt = attestedAt;
	}

	public OffsetDateTime getLastDailyRunAt() {
		return lastDailyRunAt;
	}

	public void setLastDailyRunAt(final OffsetDateTime lastDailyRunAt) {
		this.lastDailyRunAt = lastDailyRunAt;
	}

	public List<FaChild> getChildren() {
		return children;
	}

	public void setChildren(final List<FaChild> children) {
		this.children = children;
	}

	public List<FaCost> getCosts() {
		return costs;
	}

	public void setCosts(final List<FaCost> costs) {
		this.costs = costs;
	}

	public List<FaIncome> getIncomes() {
		return incomes;
	}

	public void setIncomes(final List<FaIncome> incomes) {
		this.incomes = incomes;
	}

	public List<FaPendingBenefit> getPendingBenefits() {
		return pendingBenefits;
	}

	public void setPendingBenefits(final List<FaPendingBenefit> pendingBenefits) {
		this.pendingBenefits = pendingBenefits;
	}

	public List<FaAsset> getAssets() {
		return assets;
	}

	public void setAssets(final List<FaAsset> assets) {
		this.assets = assets;
	}

	public List<FaPerson> getPersons() {
		return persons;
	}

	public void setPersons(final List<FaPerson> persons) {
		this.persons = persons;
	}

	public List<FaPlanning> getPlannings() {
		return plannings;
	}

	public void setPlannings(final List<FaPlanning> plannings) {
		this.plannings = plannings;
	}

	public List<FaPlannedActivity> getPlannedActivities() {
		return plannedActivities;
	}

	public void setPlannedActivities(final List<FaPlannedActivity> plannedActivities) {
		this.plannedActivities = plannedActivities;
	}

	public List<FaJobApplication> getJobApplications() {
		return jobApplications;
	}

	public void setJobApplications(final List<FaJobApplication> jobApplications) {
		this.jobApplications = jobApplications;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public FinancialAssistanceEntity withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public FinancialAssistanceEntity withApplicationType(final String v) {
		this.applicationType = v;
		return this;
	}

	public FinancialAssistanceEntity withMaritalStatus(final String v) {
		this.maritalStatus = v;
		return this;
	}

	public FinancialAssistanceEntity withPeriodMonth(final Integer v) {
		this.periodMonth = v;
		return this;
	}

	public FinancialAssistanceEntity withPeriodYear(final Integer v) {
		this.periodYear = v;
		return this;
	}

	public FinancialAssistanceEntity withPeriodChoice(final String v) {
		this.periodChoice = v;
		return this;
	}

	public FinancialAssistanceEntity withNormType(final String v) {
		this.normType = v;
		return this;
	}

	public FinancialAssistanceEntity withOtherBenefitDescription(final String v) {
		this.otherBenefitDescription = v;
		return this;
	}

	public FinancialAssistanceEntity withLivelihoodDescription(final String v) {
		this.livelihoodDescription = v;
		return this;
	}

	public FinancialAssistanceEntity withHasChildrenUnder21(final Boolean v) {
		this.hasChildrenUnder21 = v;
		return this;
	}

	public FinancialAssistanceEntity withChildrenResidenceChanged(final Boolean v) {
		this.childrenResidenceChanged = v;
		return this;
	}

	public FinancialAssistanceEntity withChildrenResidenceChangeDescription(final String v) {
		this.childrenResidenceChangeDescription = v;
		return this;
	}

	public FinancialAssistanceEntity withHousingForm(final String v) {
		this.housingForm = v;
		return this;
	}

	public FinancialAssistanceEntity withHousingPersonCount(final Integer v) {
		this.housingPersonCount = v;
		return this;
	}

	public FinancialAssistanceEntity withHousingRoomsPlusKitchen(final Integer v) {
		this.housingRoomsPlusKitchen = v;
		return this;
	}

	public FinancialAssistanceEntity withHousingDescription(final String v) {
		this.housingDescription = v;
		return this;
	}

	public FinancialAssistanceEntity withHousingChanged(final Boolean v) {
		this.housingChanged = v;
		return this;
	}

	public FinancialAssistanceEntity withHousingChangeDescription(final String v) {
		this.housingChangeDescription = v;
		return this;
	}

	public FinancialAssistanceEntity withHasIncomes(final Boolean v) {
		this.hasIncomes = v;
		return this;
	}

	public FinancialAssistanceEntity withHasPendingBenefits(final Boolean v) {
		this.hasPendingBenefits = v;
		return this;
	}

	public FinancialAssistanceEntity withHasAssets(final Boolean v) {
		this.hasAssets = v;
		return this;
	}

	public FinancialAssistanceEntity withStaysInMunicipality(final Boolean v) {
		this.staysInMunicipality = v;
		return this;
	}

	public FinancialAssistanceEntity withStayDescription(final String v) {
		this.stayDescription = v;
		return this;
	}

	public FinancialAssistanceEntity withAttestation(final Boolean v) {
		this.attestation = v;
		return this;
	}

	public FinancialAssistanceEntity withAttestedAt(final OffsetDateTime v) {
		this.attestedAt = v;
		return this;
	}

	public FinancialAssistanceEntity withLastDailyRunAt(final OffsetDateTime v) {
		this.lastDailyRunAt = v;
		return this;
	}

	public FinancialAssistanceEntity withChildren(final List<FaChild> v) {
		this.children = v;
		return this;
	}

	public FinancialAssistanceEntity withCosts(final List<FaCost> v) {
		this.costs = v;
		return this;
	}

	public FinancialAssistanceEntity withIncomes(final List<FaIncome> v) {
		this.incomes = v;
		return this;
	}

	public FinancialAssistanceEntity withPendingBenefits(final List<FaPendingBenefit> v) {
		this.pendingBenefits = v;
		return this;
	}

	public FinancialAssistanceEntity withAssets(final List<FaAsset> v) {
		this.assets = v;
		return this;
	}

	public FinancialAssistanceEntity withPersons(final List<FaPerson> v) {
		this.persons = v;
		return this;
	}

	public FinancialAssistanceEntity withPlannings(final List<FaPlanning> v) {
		this.plannings = v;
		return this;
	}

	public FinancialAssistanceEntity withPlannedActivities(final List<FaPlannedActivity> v) {
		this.plannedActivities = v;
		return this;
	}

	public FinancialAssistanceEntity withJobApplications(final List<FaJobApplication> v) {
		this.jobApplications = v;
		return this;
	}

	public FinancialAssistanceEntity withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	public FinancialAssistanceEntity withModified(final OffsetDateTime v) {
		this.modified = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinancialAssistanceEntity that = (FinancialAssistanceEntity) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicationType, that.applicationType)
			&& Objects.equals(maritalStatus, that.maritalStatus) && Objects.equals(periodMonth, that.periodMonth)
			&& Objects.equals(periodYear, that.periodYear) && Objects.equals(periodChoice, that.periodChoice)
			&& Objects.equals(normType, that.normType) && Objects.equals(otherBenefitDescription, that.otherBenefitDescription)
			&& Objects.equals(livelihoodDescription, that.livelihoodDescription) && Objects.equals(hasChildrenUnder21, that.hasChildrenUnder21)
			&& Objects.equals(childrenResidenceChanged, that.childrenResidenceChanged)
			&& Objects.equals(childrenResidenceChangeDescription, that.childrenResidenceChangeDescription)
			&& Objects.equals(housingForm, that.housingForm) && Objects.equals(housingPersonCount, that.housingPersonCount)
			&& Objects.equals(housingRoomsPlusKitchen, that.housingRoomsPlusKitchen)
			&& Objects.equals(housingDescription, that.housingDescription) && Objects.equals(housingChanged, that.housingChanged)
			&& Objects.equals(housingChangeDescription, that.housingChangeDescription) && Objects.equals(hasIncomes, that.hasIncomes)
			&& Objects.equals(hasPendingBenefits, that.hasPendingBenefits) && Objects.equals(hasAssets, that.hasAssets)
			&& Objects.equals(staysInMunicipality, that.staysInMunicipality) && Objects.equals(stayDescription, that.stayDescription)
			&& Objects.equals(attestation, that.attestation) && Objects.equals(attestedAt, that.attestedAt)
			&& Objects.equals(lastDailyRunAt, that.lastDailyRunAt)
			&& Objects.equals(children, that.children) && Objects.equals(costs, that.costs) && Objects.equals(incomes, that.incomes)
			&& Objects.equals(pendingBenefits, that.pendingBenefits) && Objects.equals(assets, that.assets)
			&& Objects.equals(persons, that.persons) && Objects.equals(plannings, that.plannings)
			&& Objects.equals(plannedActivities, that.plannedActivities) && Objects.equals(jobApplications, that.jobApplications)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationType, maritalStatus, periodMonth, periodYear, periodChoice, normType,
			otherBenefitDescription, livelihoodDescription, hasChildrenUnder21, childrenResidenceChanged, childrenResidenceChangeDescription,
			housingForm, housingPersonCount, housingRoomsPlusKitchen, housingDescription, housingChanged,
			housingChangeDescription, hasIncomes, hasPendingBenefits, hasAssets, staysInMunicipality, stayDescription, attestation,
			attestedAt, lastDailyRunAt, children, costs, incomes, pendingBenefits, assets, persons, plannings, plannedActivities, jobApplications,
			created, modified);
	}

	@Override
	public String toString() {
		return "FinancialAssistanceEntity{errandId='" + errandId + "', applicationType='" + applicationType
			+ "', maritalStatus='" + maritalStatus + "', periodMonth=" + periodMonth + ", periodYear=" + periodYear
			+ ", normType='" + normType + "', housingForm='" + housingForm + "', attestation=" + attestation
			+ ", lastDailyRunAt=" + lastDailyRunAt + ", created=" + created + ", modified=" + modified + '}';
	}
}
