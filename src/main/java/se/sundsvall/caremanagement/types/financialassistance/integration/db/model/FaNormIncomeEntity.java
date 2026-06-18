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
 * One income row of the normberäkning draft — an FC income type for a single recipient (applicant or co-applicant). The
 * amount the process decided ({@code processAmount}, written only by the daily prepare from the classified SSBTEK
 * incomes) is kept separate from the amount a handläggare decided ({@code handlaggareAmount}, written only from
 * Draken);
 * the effective amount posted to Lifecare is the handläggare amount when set, otherwise the process amount. A row can
 * be
 * soft-deleted ({@code deleted}) and is then excluded from the calculation but never resurrected by the daily refresh.
 * Subtracted from the norm.
 */
@Entity
@Table(name = "errand_fa_norm_income", indexes = {
	@Index(name = "idx_fa_norm_income_errand", columnList = "errand_id")
})
public class FaNormIncomeEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "origin")
	private String origin;

	@Column(name = "type_id")
	private Integer typeId;

	@Column(name = "type_name")
	private String typeName;

	@Column(name = "recipient")
	private String recipient;

	@Column(name = "process_amount", precision = 12, scale = 2)
	private BigDecimal processAmount;

	@Column(name = "process_amount_date")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime processAmountDate;

	@Column(name = "handlaggare_amount", precision = 12, scale = 2)
	private BigDecimal handlaggareAmount;

	@Column(name = "handlaggare_amount_date")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime handlaggareAmountDate;

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

	public static FaNormIncomeEntity create() {
		return new FaNormIncomeEntity();
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

	public FaNormIncomeEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaNormIncomeEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public FaNormIncomeEntity withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(final Integer typeId) {
		this.typeId = typeId;
	}

	public FaNormIncomeEntity withTypeId(final Integer typeId) {
		this.typeId = typeId;
		return this;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	public FaNormIncomeEntity withTypeName(final String typeName) {
		this.typeName = typeName;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public FaNormIncomeEntity withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public BigDecimal getProcessAmount() {
		return processAmount;
	}

	public void setProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
	}

	public FaNormIncomeEntity withProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
		return this;
	}

	public OffsetDateTime getProcessAmountDate() {
		return processAmountDate;
	}

	public void setProcessAmountDate(final OffsetDateTime processAmountDate) {
		this.processAmountDate = processAmountDate;
	}

	public FaNormIncomeEntity withProcessAmountDate(final OffsetDateTime processAmountDate) {
		this.processAmountDate = processAmountDate;
		return this;
	}

	public BigDecimal getHandlaggareAmount() {
		return handlaggareAmount;
	}

	public void setHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
	}

	public FaNormIncomeEntity withHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
		return this;
	}

	public OffsetDateTime getHandlaggareAmountDate() {
		return handlaggareAmountDate;
	}

	public void setHandlaggareAmountDate(final OffsetDateTime handlaggareAmountDate) {
		this.handlaggareAmountDate = handlaggareAmountDate;
	}

	public FaNormIncomeEntity withHandlaggareAmountDate(final OffsetDateTime handlaggareAmountDate) {
		this.handlaggareAmountDate = handlaggareAmountDate;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public FaNormIncomeEntity withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public FaNormIncomeEntity withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaNormIncomeEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaNormIncomeEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaNormIncomeEntity that = (FaNormIncomeEntity) o;
		return deleted == that.deleted && Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(origin, that.origin)
			&& Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName) && Objects.equals(recipient, that.recipient)
			&& Objects.equals(processAmount, that.processAmount) && Objects.equals(processAmountDate, that.processAmountDate)
			&& Objects.equals(handlaggareAmount, that.handlaggareAmount) && Objects.equals(handlaggareAmountDate, that.handlaggareAmountDate)
			&& Objects.equals(note, that.note) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, origin, typeId, typeName, recipient, processAmount, processAmountDate, handlaggareAmount, handlaggareAmountDate, deleted,
			note, created, updated);
	}

	@Override
	public String toString() {
		return "FaNormIncomeEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", origin='" + origin + '\'' +
			", typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", recipient='" + recipient + '\'' +
			", processAmount=" + processAmount +
			", processAmountDate=" + processAmountDate +
			", handlaggareAmount=" + handlaggareAmount +
			", handlaggareAmountDate=" + handlaggareAmountDate +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
