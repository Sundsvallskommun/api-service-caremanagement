package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * One expense row of the normberäkning draft — a single applied cost. {@code appliedAmount} is what the citizen asked
 * for (immutable, from the application); {@code processAmount} is what the regelverk allowed (the
 * {@code Decision_utgiftRegelverk} cap, written only by the daily prepare); {@code handlaggareAmount} is the override a
 * handläggare decided (written only from Draken). The effective amount posted to Lifecare is the handläggare amount
 * when
 * set, otherwise the process amount. A row can be soft-deleted ({@code deleted}) and is then excluded from the
 * calculation but never resurrected by the daily refresh. Added to the norm.
 */
@Entity
@Table(name = "errand_fa_norm_expense", indexes = {
	@Index(name = "idx_fa_norm_expense_errand", columnList = "errand_id")
})
public class FaNormExpenseEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "origin")
	private String origin;

	@Column(name = "cost_type")
	private String costType;

	@Column(name = "other_sub_type")
	private String otherSubType;

	@Column(name = "specification", length = LONG32)
	private String specification;

	@Column(name = "applied_amount", precision = 12, scale = 2)
	private BigDecimal appliedAmount;

	@Column(name = "process_amount", precision = 12, scale = 2)
	private BigDecimal processAmount;

	@Column(name = "handlaggare_amount", precision = 12, scale = 2)
	private BigDecimal handlaggareAmount;

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

	public static FaNormExpenseEntity create() {
		return new FaNormExpenseEntity();
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

	public FaNormExpenseEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaNormExpenseEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public FaNormExpenseEntity withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public String getCostType() {
		return costType;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public FaNormExpenseEntity withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public FaNormExpenseEntity withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public FaNormExpenseEntity withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public FaNormExpenseEntity withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public BigDecimal getProcessAmount() {
		return processAmount;
	}

	public void setProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
	}

	public FaNormExpenseEntity withProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
		return this;
	}

	public BigDecimal getHandlaggareAmount() {
		return handlaggareAmount;
	}

	public void setHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
	}

	public FaNormExpenseEntity withHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public FaNormExpenseEntity withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public FaNormExpenseEntity withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaNormExpenseEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaNormExpenseEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaNormExpenseEntity that = (FaNormExpenseEntity) o;
		return deleted == that.deleted && Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(origin, that.origin)
			&& Objects.equals(costType, that.costType) && Objects.equals(otherSubType, that.otherSubType) && Objects.equals(specification, that.specification)
			&& Objects.equals(appliedAmount, that.appliedAmount) && Objects.equals(processAmount, that.processAmount)
			&& Objects.equals(handlaggareAmount, that.handlaggareAmount) && Objects.equals(note, that.note) && Objects.equals(created, that.created)
			&& Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, origin, costType, otherSubType, specification, appliedAmount, processAmount, handlaggareAmount, deleted, note, created,
			updated);
	}

	@Override
	public String toString() {
		return "FaNormExpenseEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", origin='" + origin + '\'' +
			", costType='" + costType + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", appliedAmount=" + appliedAmount +
			", processAmount=" + processAmount +
			", handlaggareAmount=" + handlaggareAmount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
