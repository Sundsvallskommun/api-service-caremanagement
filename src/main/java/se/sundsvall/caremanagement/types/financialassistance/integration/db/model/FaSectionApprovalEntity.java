package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A caseworker's approval of one section of the Draken EB view — {@code CALCULATION} (calculation),
 * {@code PAYMENT} (payment) or {@code DECISION} (decision). One row per {@code (errandId, section)}; the section is
 * either approved or its approval withdrawn. {@code approvedBy} / {@code approvedAt} record who approved it and when,
 * and are cleared when the approval is withdrawn.
 */
@Entity
@Table(name = "errand_financial_assistance_section_approval", indexes = {
	@Index(name = "idx_fa_section_approval_errand", columnList = "errand_id")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_fa_section_approval", columnNames = {
		"errand_id", "section"
	})
})
public class FaSectionApprovalEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "section")
	private String section;

	@Column(name = "approved")
	private boolean approved;

	@Column(name = "approved_by")
	private String approvedBy;

	@Column(name = "approved_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime approvedAt;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaSectionApprovalEntity create() {
		return new FaSectionApprovalEntity();
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

	public FaSectionApprovalEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaSectionApprovalEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getSection() {
		return section;
	}

	public void setSection(final String section) {
		this.section = section;
	}

	public FaSectionApprovalEntity withSection(final String section) {
		this.section = section;
		return this;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(final boolean approved) {
		this.approved = approved;
	}

	public FaSectionApprovalEntity withApproved(final boolean approved) {
		this.approved = approved;
		return this;
	}

	public String getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(final String approvedBy) {
		this.approvedBy = approvedBy;
	}

	public FaSectionApprovalEntity withApprovedBy(final String approvedBy) {
		this.approvedBy = approvedBy;
		return this;
	}

	public OffsetDateTime getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(final OffsetDateTime approvedAt) {
		this.approvedAt = approvedAt;
	}

	public FaSectionApprovalEntity withApprovedAt(final OffsetDateTime approvedAt) {
		this.approvedAt = approvedAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaSectionApprovalEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaSectionApprovalEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaSectionApprovalEntity that = (FaSectionApprovalEntity) o;
		return approved == that.approved && Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(section, that.section) && Objects.equals(approvedBy, that.approvedBy)
			&& Objects.equals(approvedAt, that.approvedAt) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, section, approved, approvedBy, approvedAt, created, updated);
	}

	@Override
	public String toString() {
		return "FaSectionApprovalEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", section='" + section + '\'' +
			", approved=" + approved +
			", approvedBy='" + approvedBy + '\'' +
			", approvedAt=" + approvedAt +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
