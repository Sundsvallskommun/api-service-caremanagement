package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A single EB bevakning on an errand — a date-bound watch/reminder the handläggare manages in Draken. Unlike the income
 * warnings it carries no acknowledge lifecycle: it is created, edited and removed directly, and has a start date (when
 * the watch becomes relevant) plus an optional end date. Modelled after Lifecare IFO's "Bevakningar".
 */
@Entity
@Table(name = "errand_financial_assistance_bevakning", indexes = {
	@Index(name = "idx_fa_bevakning_errand", columnList = "errand_id")
})
public class FaBevakningEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "title")
	private String title;

	@Column(name = "description", length = LONG32)
	private String description;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaBevakningEntity create() {
		return new FaBevakningEntity();
	}

	@PrePersist
	void prePersist() {
		final var now = OffsetDateTime.now();
		created = now;
		updated = now;
	}

	@PreUpdate
	void preUpdate() {
		updated = OffsetDateTime.now();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FaBevakningEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaBevakningEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public FaBevakningEntity withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public FaBevakningEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(final LocalDate startDate) {
		this.startDate = startDate;
	}

	public FaBevakningEntity withStartDate(final LocalDate startDate) {
		this.startDate = startDate;
		return this;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(final LocalDate endDate) {
		this.endDate = endDate;
	}

	public FaBevakningEntity withEndDate(final LocalDate endDate) {
		this.endDate = endDate;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public FaBevakningEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaBevakningEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaBevakningEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaBevakningEntity that = (FaBevakningEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(title, that.title)
			&& Objects.equals(description, that.description) && Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate)
			&& Objects.equals(createdBy, that.createdBy) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, title, description, startDate, endDate, createdBy, created, updated);
	}

	@Override
	public String toString() {
		return "FaBevakningEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", startDate=" + startDate +
			", endDate=" + endDate +
			", createdBy='" + createdBy + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
