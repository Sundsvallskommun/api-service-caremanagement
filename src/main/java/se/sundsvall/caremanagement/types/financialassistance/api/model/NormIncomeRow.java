package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * One income row of the normberäkning draft, as returned to Draken — one FC income type with a sökande (applicant, "S")
 * side and a medsökande (co-applicant, "M") side, mirroring the Lifecare INKOMSTER tab and FC
 * {@code CalculationIncomes}. Per side the amount the process decided ({@code *ProcessAmount}, from the classified
 * SSBTEK income) is read-only; the handläggare's override ({@code *HandlaggareAmount}) and the note are editable. The
 * effective amount per side ({@code *EffectiveAmount}) is what is posted to Lifecare = the handläggare amount when set,
 * otherwise the process amount. Subtracted from the norm.
 */
@Schema(description = "One income row of the normberäkning draft (FC income type with applicant/co-applicant sides, process vs handläggare amounts).")
public class NormIncomeRow {

	@Schema(description = "The row id", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Who created the row: the process or a handläggare", allowableValues = {
		"SYSTEM", "HANDLAGGARE"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String origin;

	@Schema(description = "The FC income-type id", examples = "20", accessMode = Schema.AccessMode.READ_ONLY)
	private Integer typeId;

	@Schema(description = "The FC income-type name", examples = "Bostadsbidrag", accessMode = Schema.AccessMode.READ_ONLY)
	private String typeName;

	@Schema(description = "The amount the process decided for the applicant (from the classified SSBTEK income)", examples = "1850.00", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal applicantProcessAmount;

	@Schema(description = "The amount a handläggare decided for the applicant; overrides the process amount when set", examples = "1900.00")
	private BigDecimal applicantHandlaggareAmount;

	@Schema(description = "The amount actually used for the applicant (handläggare amount when set, otherwise process amount)", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal applicantEffectiveAmount;

	@Schema(description = "The date the applicant amount is attributed to")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime applicantAmountDate;

	@Schema(description = "The amount the process decided for the co-applicant (from the classified SSBTEK income)", examples = "1850.00", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal coapplicantProcessAmount;

	@Schema(description = "The amount a handläggare decided for the co-applicant; overrides the process amount when set", examples = "1900.00")
	private BigDecimal coapplicantHandlaggareAmount;

	@Schema(description = "The amount actually used for the co-applicant (handläggare amount when set, otherwise process amount)", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal coapplicantEffectiveAmount;

	@Schema(description = "The date the co-applicant amount is attributed to")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime coapplicantAmountDate;

	@Schema(description = "Whether the row is soft-deleted (excluded from the calculation, not resurrected by the daily refresh)", accessMode = Schema.AccessMode.READ_ONLY)
	private boolean deleted;

	@Schema(description = "Free-text note", examples = "SSBTEK: Bostadsbidrag")
	private String note;

	@Schema(description = "When the row was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the row was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static NormIncomeRow create() {
		return new NormIncomeRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormIncomeRow withId(final String id) {
		this.id = id;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormIncomeRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(final Integer typeId) {
		this.typeId = typeId;
	}

	public NormIncomeRow withTypeId(final Integer typeId) {
		this.typeId = typeId;
		return this;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	public NormIncomeRow withTypeName(final String typeName) {
		this.typeName = typeName;
		return this;
	}

	public BigDecimal getApplicantProcessAmount() {
		return applicantProcessAmount;
	}

	public void setApplicantProcessAmount(final BigDecimal applicantProcessAmount) {
		this.applicantProcessAmount = applicantProcessAmount;
	}

	public NormIncomeRow withApplicantProcessAmount(final BigDecimal applicantProcessAmount) {
		this.applicantProcessAmount = applicantProcessAmount;
		return this;
	}

	public BigDecimal getApplicantHandlaggareAmount() {
		return applicantHandlaggareAmount;
	}

	public void setApplicantHandlaggareAmount(final BigDecimal applicantHandlaggareAmount) {
		this.applicantHandlaggareAmount = applicantHandlaggareAmount;
	}

	public NormIncomeRow withApplicantHandlaggareAmount(final BigDecimal applicantHandlaggareAmount) {
		this.applicantHandlaggareAmount = applicantHandlaggareAmount;
		return this;
	}

	public BigDecimal getApplicantEffectiveAmount() {
		return applicantEffectiveAmount;
	}

	public void setApplicantEffectiveAmount(final BigDecimal applicantEffectiveAmount) {
		this.applicantEffectiveAmount = applicantEffectiveAmount;
	}

	public NormIncomeRow withApplicantEffectiveAmount(final BigDecimal applicantEffectiveAmount) {
		this.applicantEffectiveAmount = applicantEffectiveAmount;
		return this;
	}

	public OffsetDateTime getApplicantAmountDate() {
		return applicantAmountDate;
	}

	public void setApplicantAmountDate(final OffsetDateTime applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
	}

	public NormIncomeRow withApplicantAmountDate(final OffsetDateTime applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
		return this;
	}

	public BigDecimal getCoapplicantProcessAmount() {
		return coapplicantProcessAmount;
	}

	public void setCoapplicantProcessAmount(final BigDecimal coapplicantProcessAmount) {
		this.coapplicantProcessAmount = coapplicantProcessAmount;
	}

	public NormIncomeRow withCoapplicantProcessAmount(final BigDecimal coapplicantProcessAmount) {
		this.coapplicantProcessAmount = coapplicantProcessAmount;
		return this;
	}

	public BigDecimal getCoapplicantHandlaggareAmount() {
		return coapplicantHandlaggareAmount;
	}

	public void setCoapplicantHandlaggareAmount(final BigDecimal coapplicantHandlaggareAmount) {
		this.coapplicantHandlaggareAmount = coapplicantHandlaggareAmount;
	}

	public NormIncomeRow withCoapplicantHandlaggareAmount(final BigDecimal coapplicantHandlaggareAmount) {
		this.coapplicantHandlaggareAmount = coapplicantHandlaggareAmount;
		return this;
	}

	public BigDecimal getCoapplicantEffectiveAmount() {
		return coapplicantEffectiveAmount;
	}

	public void setCoapplicantEffectiveAmount(final BigDecimal coapplicantEffectiveAmount) {
		this.coapplicantEffectiveAmount = coapplicantEffectiveAmount;
	}

	public NormIncomeRow withCoapplicantEffectiveAmount(final BigDecimal coapplicantEffectiveAmount) {
		this.coapplicantEffectiveAmount = coapplicantEffectiveAmount;
		return this;
	}

	public OffsetDateTime getCoapplicantAmountDate() {
		return coapplicantAmountDate;
	}

	public void setCoapplicantAmountDate(final OffsetDateTime coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
	}

	public NormIncomeRow withCoapplicantAmountDate(final OffsetDateTime coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public NormIncomeRow withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormIncomeRow withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NormIncomeRow withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public NormIncomeRow withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormIncomeRow that = (NormIncomeRow) o;
		return deleted == that.deleted && Objects.equals(id, that.id) && Objects.equals(origin, that.origin) && Objects.equals(typeId, that.typeId)
			&& Objects.equals(typeName, that.typeName) && Objects.equals(applicantProcessAmount, that.applicantProcessAmount)
			&& Objects.equals(applicantHandlaggareAmount, that.applicantHandlaggareAmount) && Objects.equals(applicantEffectiveAmount, that.applicantEffectiveAmount)
			&& Objects.equals(applicantAmountDate, that.applicantAmountDate) && Objects.equals(coapplicantProcessAmount, that.coapplicantProcessAmount)
			&& Objects.equals(coapplicantHandlaggareAmount, that.coapplicantHandlaggareAmount)
			&& Objects.equals(coapplicantEffectiveAmount, that.coapplicantEffectiveAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate)
			&& Objects.equals(note, that.note) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, origin, typeId, typeName, applicantProcessAmount, applicantHandlaggareAmount, applicantEffectiveAmount, applicantAmountDate,
			coapplicantProcessAmount, coapplicantHandlaggareAmount, coapplicantEffectiveAmount, coapplicantAmountDate, deleted, note, created, updated);
	}

	@Override
	public String toString() {
		return "NormIncomeRow{" +
			"id='" + id + '\'' +
			", origin='" + origin + '\'' +
			", typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantProcessAmount=" + applicantProcessAmount +
			", applicantHandlaggareAmount=" + applicantHandlaggareAmount +
			", applicantEffectiveAmount=" + applicantEffectiveAmount +
			", applicantAmountDate=" + applicantAmountDate +
			", coapplicantProcessAmount=" + coapplicantProcessAmount +
			", coapplicantHandlaggareAmount=" + coapplicantHandlaggareAmount +
			", coapplicantEffectiveAmount=" + coapplicantEffectiveAmount +
			", coapplicantAmountDate=" + coapplicantAmountDate +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
