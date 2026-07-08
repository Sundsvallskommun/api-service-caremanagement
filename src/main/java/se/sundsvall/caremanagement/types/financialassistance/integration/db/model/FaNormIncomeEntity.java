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
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * One income row of the calculation draft — one FC income type, with a applicant (applicant, "S") side and a
 * co-applicant
 * (co-applicant, "M") side, mirroring the Lifecare INCOMES tab and FC {@code CalculationIncomes}. Each side keeps the
 * amount the process decided (from the classified SSBTEK income, written only by the daily prepare) separate from the
 * amount a caseworker decided (written only from Draken); the effective amount per side is the caseworker amount when
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

	@Column(name = "position")
	private Integer position;

	@Column(name = "type_id")
	private Integer typeId;

	@Column(name = "type_name")
	private String typeName;

	@Column(name = "applicant_process_amount", precision = 12, scale = 2)
	private BigDecimal applicantProcessAmount;

	@Column(name = "applicant_caseworker_amount", precision = 12, scale = 2)
	private BigDecimal applicantCaseworkerAmount;

	@Column(name = "applicant_amount_date")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime applicantAmountDate;

	@Column(name = "coapplicant_process_amount", precision = 12, scale = 2)
	private BigDecimal coapplicantProcessAmount;

	@Column(name = "coapplicant_caseworker_amount", precision = 12, scale = 2)
	private BigDecimal coapplicantCaseworkerAmount;

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

	public Integer getPosition() {
		return position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}

	public FaNormIncomeEntity withPosition(final Integer position) {
		this.position = position;
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

	public BigDecimal getApplicantCaseworkerAmount() {
		return applicantCaseworkerAmount;
	}

	public void setApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
	}

	public FaNormIncomeEntity withApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
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

	public BigDecimal getCoapplicantCaseworkerAmount() {
		return coapplicantCaseworkerAmount;
	}

	public void setCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
	}

	public FaNormIncomeEntity withCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
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

	// 'note' (a LONG32 column) is deliberately excluded from equals/hashCode/toString — it can be large and is not part of
	// the entity's identity.
	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaNormIncomeEntity that = (FaNormIncomeEntity) o;
		return deleted == that.deleted && Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(origin, that.origin)
			&& Objects.equals(position, that.position)
			&& Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName)
			&& Objects.equals(applicantProcessAmount, that.applicantProcessAmount) && Objects.equals(applicantCaseworkerAmount, that.applicantCaseworkerAmount)
			&& Objects.equals(applicantAmountDate, that.applicantAmountDate) && Objects.equals(coapplicantProcessAmount, that.coapplicantProcessAmount)
			&& Objects.equals(coapplicantCaseworkerAmount, that.coapplicantCaseworkerAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, origin, position, typeId, typeName, applicantProcessAmount, applicantCaseworkerAmount, applicantAmountDate,
			coapplicantProcessAmount, coapplicantCaseworkerAmount, coapplicantAmountDate, deleted, created, updated);
	}

	@Override
	public String toString() {
		return "FaNormIncomeEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", origin='" + origin + '\'' +
			", position=" + position +
			", typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantProcessAmount=" + applicantProcessAmount +
			", applicantCaseworkerAmount=" + applicantCaseworkerAmount +
			", applicantAmountDate=" + applicantAmountDate +
			", coapplicantProcessAmount=" + coapplicantProcessAmount +
			", coapplicantCaseworkerAmount=" + coapplicantCaseworkerAmount +
			", coapplicantAmountDate=" + coapplicantAmountDate +
			", deleted=" + deleted +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
