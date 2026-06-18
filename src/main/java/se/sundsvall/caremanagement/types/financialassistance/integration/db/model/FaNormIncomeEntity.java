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
 * One income row of the normberäkning draft — one FC income type, with a sökande (applicant, "S") side and a medsökande
 * (co-applicant, "M") side, mirroring the Lifecare INKOMSTER tab and FC {@code CalculationIncomes}. Each side keeps the
 * amount the process decided (from the classified SSBTEK income, written only by the daily prepare) separate from the
 * amount a handläggare decided (written only from Draken); the effective amount per side is the handläggare amount when
 * set, otherwise the process amount. A row can be soft-deleted and is then excluded but never resurrected by the daily
 * refresh. Subtracted from the norm.
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

	@Column(name = "applicant_process_amount", precision = 12, scale = 2)
	private BigDecimal applicantProcessAmount;

	@Column(name = "applicant_handlaggare_amount", precision = 12, scale = 2)
	private BigDecimal applicantHandlaggareAmount;

	@Column(name = "applicant_amount_date")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime applicantAmountDate;

	@Column(name = "coapplicant_process_amount", precision = 12, scale = 2)
	private BigDecimal coapplicantProcessAmount;

	@Column(name = "coapplicant_handlaggare_amount", precision = 12, scale = 2)
	private BigDecimal coapplicantHandlaggareAmount;

	@Column(name = "coapplicant_amount_date")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime coapplicantAmountDate;

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

	public BigDecimal getApplicantProcessAmount() {
		return applicantProcessAmount;
	}

	public void setApplicantProcessAmount(final BigDecimal applicantProcessAmount) {
		this.applicantProcessAmount = applicantProcessAmount;
	}

	public FaNormIncomeEntity withApplicantProcessAmount(final BigDecimal applicantProcessAmount) {
		this.applicantProcessAmount = applicantProcessAmount;
		return this;
	}

	public BigDecimal getApplicantHandlaggareAmount() {
		return applicantHandlaggareAmount;
	}

	public void setApplicantHandlaggareAmount(final BigDecimal applicantHandlaggareAmount) {
		this.applicantHandlaggareAmount = applicantHandlaggareAmount;
	}

	public FaNormIncomeEntity withApplicantHandlaggareAmount(final BigDecimal applicantHandlaggareAmount) {
		this.applicantHandlaggareAmount = applicantHandlaggareAmount;
		return this;
	}

	public OffsetDateTime getApplicantAmountDate() {
		return applicantAmountDate;
	}

	public void setApplicantAmountDate(final OffsetDateTime applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
	}

	public FaNormIncomeEntity withApplicantAmountDate(final OffsetDateTime applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
		return this;
	}

	public BigDecimal getCoapplicantProcessAmount() {
		return coapplicantProcessAmount;
	}

	public void setCoapplicantProcessAmount(final BigDecimal coapplicantProcessAmount) {
		this.coapplicantProcessAmount = coapplicantProcessAmount;
	}

	public FaNormIncomeEntity withCoapplicantProcessAmount(final BigDecimal coapplicantProcessAmount) {
		this.coapplicantProcessAmount = coapplicantProcessAmount;
		return this;
	}

	public BigDecimal getCoapplicantHandlaggareAmount() {
		return coapplicantHandlaggareAmount;
	}

	public void setCoapplicantHandlaggareAmount(final BigDecimal coapplicantHandlaggareAmount) {
		this.coapplicantHandlaggareAmount = coapplicantHandlaggareAmount;
	}

	public FaNormIncomeEntity withCoapplicantHandlaggareAmount(final BigDecimal coapplicantHandlaggareAmount) {
		this.coapplicantHandlaggareAmount = coapplicantHandlaggareAmount;
		return this;
	}

	public OffsetDateTime getCoapplicantAmountDate() {
		return coapplicantAmountDate;
	}

	public void setCoapplicantAmountDate(final OffsetDateTime coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
	}

	public FaNormIncomeEntity withCoapplicantAmountDate(final OffsetDateTime coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
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
			&& Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName)
			&& Objects.equals(applicantProcessAmount, that.applicantProcessAmount) && Objects.equals(applicantHandlaggareAmount, that.applicantHandlaggareAmount)
			&& Objects.equals(applicantAmountDate, that.applicantAmountDate) && Objects.equals(coapplicantProcessAmount, that.coapplicantProcessAmount)
			&& Objects.equals(coapplicantHandlaggareAmount, that.coapplicantHandlaggareAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate)
			&& Objects.equals(note, that.note) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, origin, typeId, typeName, applicantProcessAmount, applicantHandlaggareAmount, applicantAmountDate,
			coapplicantProcessAmount, coapplicantHandlaggareAmount, coapplicantAmountDate, deleted, note, created, updated);
	}

	@Override
	public String toString() {
		return "FaNormIncomeEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", origin='" + origin + '\'' +
			", typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantProcessAmount=" + applicantProcessAmount +
			", applicantHandlaggareAmount=" + applicantHandlaggareAmount +
			", applicantAmountDate=" + applicantAmountDate +
			", coapplicantProcessAmount=" + coapplicantProcessAmount +
			", coapplicantHandlaggareAmount=" + coapplicantHandlaggareAmount +
			", coapplicantAmountDate=" + coapplicantAmountDate +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
