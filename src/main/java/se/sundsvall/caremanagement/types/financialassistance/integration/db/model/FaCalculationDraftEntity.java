package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The draft calculation header for an errand — one row per errand, holding the application month and the selected
 * norm. The section rows (persons, incomes, expenses) live in their own tables ({@code errand_fa_norm_person},
 * {@code errand_fa_norm_income}, {@code errand_fa_norm_expense}), each row owning its process and caseworker values
 * separately. The EB process prepares the draft each day without writing to Lifecare; on a decision the effective
 * values
 * are posted. The errand id is the primary key.
 */
@Entity
@Table(name = "errand_financial_assistance_calculation_draft")
public class FaCalculationDraftEntity {

	@Id
	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "application_month")
	private String applicationMonth;

	@Column(name = "norm_id")
	private Integer normId;

	@ElementCollection
	@CollectionTable(name = "errand_fa_calculation_draft_norm_type", joinColumns = @JoinColumn(name = "errand_id"))
	@Column(name = "norm_type", length = 32)
	private List<String> normType;

	@Column(name = "calculation_from_date")
	private LocalDate calculationFromDate;

	@Column(name = "calculation_to_date")
	private LocalDate calculationToDate;

	@Column(name = "calculation_date")
	private LocalDate calculationDate;

	@Column(name = "has_custom_household_size")
	private Boolean hasCustomHouseholdSize;

	@Column(name = "household_size")
	private Integer householdSize;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaCalculationDraftEntity create() {
		return new FaCalculationDraftEntity();
	}

	@PrePersist
	void prePersist() {
		final var now = OffsetDateTime.now(ZoneId.systemDefault());
		created = now;
		updated = now;
	}

	@PreUpdate
	void preUpdate() {
		updated = OffsetDateTime.now(ZoneId.systemDefault());
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaCalculationDraftEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public FaCalculationDraftEntity withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public Integer getNormId() {
		return normId;
	}

	public void setNormId(final Integer normId) {
		this.normId = normId;
	}

	public FaCalculationDraftEntity withNormId(final Integer normId) {
		this.normId = normId;
		return this;
	}

	public List<String> getNormType() {
		return normType;
	}

	public void setNormType(final List<String> normType) {
		this.normType = normType;
	}

	public FaCalculationDraftEntity withNormType(final List<String> normType) {
		this.normType = normType;
		return this;
	}

	public LocalDate getCalculationFromDate() {
		return calculationFromDate;
	}

	public void setCalculationFromDate(final LocalDate calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
	}

	public FaCalculationDraftEntity withCalculationFromDate(final LocalDate calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
		return this;
	}

	public LocalDate getCalculationToDate() {
		return calculationToDate;
	}

	public void setCalculationToDate(final LocalDate calculationToDate) {
		this.calculationToDate = calculationToDate;
	}

	public FaCalculationDraftEntity withCalculationToDate(final LocalDate calculationToDate) {
		this.calculationToDate = calculationToDate;
		return this;
	}

	public LocalDate getCalculationDate() {
		return calculationDate;
	}

	public void setCalculationDate(final LocalDate calculationDate) {
		this.calculationDate = calculationDate;
	}

	public FaCalculationDraftEntity withCalculationDate(final LocalDate calculationDate) {
		this.calculationDate = calculationDate;
		return this;
	}

	public Boolean getHasCustomHouseholdSize() {
		return hasCustomHouseholdSize;
	}

	public void setHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
	}

	public FaCalculationDraftEntity withHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
		return this;
	}

	public Integer getHouseholdSize() {
		return householdSize;
	}

	public void setHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
	}

	public FaCalculationDraftEntity withHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaCalculationDraftEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaCalculationDraftEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaCalculationDraftEntity that = (FaCalculationDraftEntity) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth)
			&& Objects.equals(normId, that.normId) && Objects.equals(normType, that.normType)
			&& Objects.equals(calculationFromDate, that.calculationFromDate) && Objects.equals(calculationToDate, that.calculationToDate)
			&& Objects.equals(calculationDate, that.calculationDate) && Objects.equals(hasCustomHouseholdSize, that.hasCustomHouseholdSize)
			&& Objects.equals(householdSize, that.householdSize) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, normId, normType, calculationFromDate, calculationToDate, calculationDate, hasCustomHouseholdSize, householdSize,
			created, updated);
	}

	@Override
	public String toString() {
		return "FaCalculationDraftEntity{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", normId=" + normId +
			", normType=" + normType +
			", calculationFromDate=" + calculationFromDate +
			", calculationToDate=" + calculationToDate +
			", calculationDate=" + calculationDate +
			", hasCustomHouseholdSize=" + hasCustomHouseholdSize +
			", householdSize=" + householdSize +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
