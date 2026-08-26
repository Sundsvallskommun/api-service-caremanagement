package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A jobbstimulans period on an errand, mirrored out of Lifecare by the RPA supplements ingest. Lifecare's own
 * {@code jobStimulusId} is deliberately not stored: Lifecare deletes and recreates the whole period set on every save,
 * so its ids are one-shot — each delivery therefore replaces the errand's full period set instead of upserting rows.
 * {@code role} says whose period it is (the applicant's or the co-applicant's).
 */
@Entity
@Table(name = "errand_fa_job_stimulus_period", indexes = {
	@Index(name = "idx_fa_job_stimulus_period_errand_id", columnList = "errand_id")
})
public class FaJobStimulusPeriodEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "role", length = 16)
	private String role;

	@Column(name = "from_date")
	private LocalDate fromDate;

	@Column(name = "to_date")
	private LocalDate toDate;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static FaJobStimulusPeriodEntity create() {
		return new FaJobStimulusPeriodEntity();
	}

	@PrePersist
	void prePersist() {
		created = OffsetDateTime.now(ZoneId.systemDefault());
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FaJobStimulusPeriodEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaJobStimulusPeriodEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public FaJobStimulusPeriodEntity withRole(final String role) {
		this.role = role;
		return this;
	}

	public LocalDate getFromDate() {
		return fromDate;
	}

	public void setFromDate(final LocalDate fromDate) {
		this.fromDate = fromDate;
	}

	public FaJobStimulusPeriodEntity withFromDate(final LocalDate fromDate) {
		this.fromDate = fromDate;
		return this;
	}

	public LocalDate getToDate() {
		return toDate;
	}

	public void setToDate(final LocalDate toDate) {
		this.toDate = toDate;
	}

	public FaJobStimulusPeriodEntity withToDate(final LocalDate toDate) {
		this.toDate = toDate;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaJobStimulusPeriodEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (!(o instanceof final FaJobStimulusPeriodEntity that))
			return false;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(role, that.role)
			&& Objects.equals(fromDate, that.fromDate) && Objects.equals(toDate, that.toDate) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, role, fromDate, toDate, created);
	}

	@Override
	public String toString() {
		return "FaJobStimulusPeriodEntity{id='" + id + "', errandId='" + errandId + "', role='" + role + "', fromDate="
			+ fromDate + ", toDate=" + toDate + ", created=" + created + "}";
	}
}
