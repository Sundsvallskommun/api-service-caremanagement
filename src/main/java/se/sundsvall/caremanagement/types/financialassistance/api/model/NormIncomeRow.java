package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * One income row of the normberäkning draft, as returned to Draken — an FC income type for a single recipient. The
 * amount the process decided ({@code processAmount}) is read-only; the handläggare's override
 * ({@code handlaggareAmount})
 * and the note are editable. {@code effectiveAmount} is what is posted to Lifecare = the handläggare amount when set,
 * otherwise the process amount. Subtracted from the norm.
 */
@Schema(description = "One income row of the normberäkning draft (FC income type + recipient, process vs handläggare amount).")
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

	@Schema(description = "Whose income this is", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String recipient;

	@Schema(description = "The amount the process decided (from the classified SSBTEK income)", examples = "1850.00", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal processAmount;

	@Schema(description = "The date the process amount is attributed to", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime processAmountDate;

	@Schema(description = "The amount a handläggare decided; overrides the process amount when set", examples = "1900.00")
	private BigDecimal handlaggareAmount;

	@Schema(description = "The date the handläggare amount is attributed to")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime handlaggareAmountDate;

	@Schema(description = "The amount actually used (handläggare amount when set, otherwise process amount)", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal effectiveAmount;

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

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public NormIncomeRow withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public BigDecimal getProcessAmount() {
		return processAmount;
	}

	public void setProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
	}

	public NormIncomeRow withProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
		return this;
	}

	public OffsetDateTime getProcessAmountDate() {
		return processAmountDate;
	}

	public void setProcessAmountDate(final OffsetDateTime processAmountDate) {
		this.processAmountDate = processAmountDate;
	}

	public NormIncomeRow withProcessAmountDate(final OffsetDateTime processAmountDate) {
		this.processAmountDate = processAmountDate;
		return this;
	}

	public BigDecimal getHandlaggareAmount() {
		return handlaggareAmount;
	}

	public void setHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
	}

	public NormIncomeRow withHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
		return this;
	}

	public OffsetDateTime getHandlaggareAmountDate() {
		return handlaggareAmountDate;
	}

	public void setHandlaggareAmountDate(final OffsetDateTime handlaggareAmountDate) {
		this.handlaggareAmountDate = handlaggareAmountDate;
	}

	public NormIncomeRow withHandlaggareAmountDate(final OffsetDateTime handlaggareAmountDate) {
		this.handlaggareAmountDate = handlaggareAmountDate;
		return this;
	}

	public BigDecimal getEffectiveAmount() {
		return effectiveAmount;
	}

	public void setEffectiveAmount(final BigDecimal effectiveAmount) {
		this.effectiveAmount = effectiveAmount;
	}

	public NormIncomeRow withEffectiveAmount(final BigDecimal effectiveAmount) {
		this.effectiveAmount = effectiveAmount;
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
			&& Objects.equals(typeName, that.typeName) && Objects.equals(recipient, that.recipient) && Objects.equals(processAmount, that.processAmount)
			&& Objects.equals(processAmountDate, that.processAmountDate) && Objects.equals(handlaggareAmount, that.handlaggareAmount)
			&& Objects.equals(handlaggareAmountDate, that.handlaggareAmountDate) && Objects.equals(effectiveAmount, that.effectiveAmount)
			&& Objects.equals(note, that.note) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, origin, typeId, typeName, recipient, processAmount, processAmountDate, handlaggareAmount, handlaggareAmountDate, effectiveAmount,
			deleted, note, created, updated);
	}

	@Override
	public String toString() {
		return "NormIncomeRow{" +
			"id='" + id + '\'' +
			", origin='" + origin + '\'' +
			", typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", recipient='" + recipient + '\'' +
			", processAmount=" + processAmount +
			", processAmountDate=" + processAmountDate +
			", handlaggareAmount=" + handlaggareAmount +
			", handlaggareAmountDate=" + handlaggareAmountDate +
			", effectiveAmount=" + effectiveAmount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
