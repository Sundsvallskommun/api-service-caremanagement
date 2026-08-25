package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * One person row of the calculation draft — a household member (applicant, co-applicant or child).
 * {@code processDays}
 * is the number of days in the home the process derived from the application (written only by the daily prepare);
 * {@code caseworkerDays} is the override a caseworker decided (written only from Draken). The effective number of
 * days
 * sent to Lifecare is the caseworker value when set, otherwise the process value. A row can be soft-deleted
 * ({@code deleted}) and is then excluded from the calculation but never resurrected by the daily refresh. Drives the
 * norm base, and is compared against the previous calculation in Lifecare to warn on household drift.
 */
@Entity
@Table(name = "errand_fa_norm_person", indexes = {
	@Index(name = "idx_fa_norm_person_errand", columnList = "errand_id")
})
public class FaNormPersonEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "origin")
	private String origin;

	@Column(name = "position")
	private Integer position;

	@Column(name = "party_id")
	private String partyId;

	@Column(name = "role")
	private String role;

	@Column(name = "name")
	private String name;

	@Column(name = "process_days")
	private Integer processDays;

	@Column(name = "caseworker_days")
	private Integer caseworkerDays;

	@Column(name = "included")
	private boolean included;

	@Column(name = "deviation_from_date")
	private LocalDate deviationFromDate;

	@Column(name = "deviation_to_date")
	private LocalDate deviationToDate;

	@Column(name = "norm_interval")
	private String normInterval;

	@Column(name = "job_stimulus_amount", precision = 12, scale = 2)
	private BigDecimal jobStimulusAmount;

	@Column(name = "deleted")
	private boolean deleted;

	@Column(name = "note", length = LONG32)
	private String note;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaNormPersonEntity create() {
		return new FaNormPersonEntity();
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

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FaNormPersonEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaNormPersonEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public FaNormPersonEntity withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}

	public FaNormPersonEntity withPosition(final Integer position) {
		this.position = position;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public FaNormPersonEntity withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public FaNormPersonEntity withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public FaNormPersonEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getProcessDays() {
		return processDays;
	}

	public void setProcessDays(final Integer processDays) {
		this.processDays = processDays;
	}

	public FaNormPersonEntity withProcessDays(final Integer processDays) {
		this.processDays = processDays;
		return this;
	}

	public Integer getCaseworkerDays() {
		return caseworkerDays;
	}

	public void setCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
	}

	public FaNormPersonEntity withCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
		return this;
	}

	public boolean isIncluded() {
		return included;
	}

	public void setIncluded(final boolean included) {
		this.included = included;
	}

	public FaNormPersonEntity withIncluded(final boolean included) {
		this.included = included;
		return this;
	}

	public LocalDate getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public FaNormPersonEntity withDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public LocalDate getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public FaNormPersonEntity withDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public String getNormInterval() {
		return normInterval;
	}

	public void setNormInterval(final String normInterval) {
		this.normInterval = normInterval;
	}

	public FaNormPersonEntity withNormInterval(final String normInterval) {
		this.normInterval = normInterval;
		return this;
	}

	public BigDecimal getJobStimulusAmount() {
		return jobStimulusAmount;
	}

	public void setJobStimulusAmount(final BigDecimal jobStimulusAmount) {
		this.jobStimulusAmount = jobStimulusAmount;
	}

	public FaNormPersonEntity withJobStimulusAmount(final BigDecimal jobStimulusAmount) {
		this.jobStimulusAmount = jobStimulusAmount;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public FaNormPersonEntity withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public FaNormPersonEntity withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaNormPersonEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaNormPersonEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	// 'note' (a LONG32 column) is deliberately excluded from equals/hashCode/toString — it can be large and is not part of
	// the entity's identity.
	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaNormPersonEntity that = (FaNormPersonEntity) o;
		return deleted == that.deleted && included == that.included && Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(origin, that.origin) && Objects.equals(position, that.position) && Objects.equals(partyId, that.partyId) && Objects.equals(role, that.role)
			&& Objects.equals(name, that.name)
			&& Objects.equals(processDays, that.processDays) && Objects.equals(caseworkerDays, that.caseworkerDays)
			&& Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate)
			&& Objects.equals(normInterval, that.normInterval) && Objects.equals(jobStimulusAmount, that.jobStimulusAmount)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, origin, position, partyId, role, name, processDays, caseworkerDays, included, deviationFromDate, deviationToDate, normInterval,
			jobStimulusAmount, deleted, created, updated);
	}

	@Override
	public String toString() {
		return "FaNormPersonEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", origin='" + origin + '\'' +
			", position=" + position +
			", partyId='" + partyId + '\'' +
			", role='" + role + '\'' +
			", name='" + name + '\'' +
			", processDays=" + processDays +
			", caseworkerDays=" + caseworkerDays +
			", included=" + included +
			", deviationFromDate=" + deviationFromDate +
			", deviationToDate=" + deviationToDate +
			", normInterval='" + normInterval + '\'' +
			", jobStimulusAmount=" + jobStimulusAmount +
			", deleted=" + deleted +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
