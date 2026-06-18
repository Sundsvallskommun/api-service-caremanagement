package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * One expense row of the normberäkning draft, as returned to Draken — a single applied cost. {@code appliedAmount} is
 * what the citizen asked for and {@code processAmount} what the regelverk allowed (both read-only); the handläggare's
 * override ({@code handlaggareAmount}) and the note are editable. {@code effectiveAmount} is what is posted to Lifecare
 * =
 * the handläggare amount when set, otherwise the process amount. Added to the norm.
 */
@Schema(description = "One expense row of the normberäkning draft (applied cost, process vs handläggare amount).")
public class NormExpenseRow {

	@Schema(description = "The row id", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Who created the row: the process or a handläggare", allowableValues = {
		"SYSTEM", "HANDLAGGARE"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String origin;

	@Schema(description = "Which Lifecare bucket the expense posts to", allowableValues = {
		"EXPENSE", "SPECIAL_EXPENSE"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String bucket;

	@Schema(description = "The cost type", accessMode = Schema.AccessMode.READ_ONLY)
	private String costType;

	@Schema(description = "The other sub-type (when the cost type is 'other')", accessMode = Schema.AccessMode.READ_ONLY)
	private String otherSubType;

	@Schema(description = "The cost specification", accessMode = Schema.AccessMode.READ_ONLY)
	private String specification;

	@Schema(description = "The amount the citizen applied for", examples = "1200.00", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal appliedAmount;

	@Schema(description = "The amount the regelverk allowed (the process amount)", examples = "1000.00", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal processAmount;

	@Schema(description = "The amount a handläggare decided; overrides the process amount when set", examples = "1100.00")
	private BigDecimal handlaggareAmount;

	@Schema(description = "The amount actually used (handläggare amount when set, otherwise process amount)", accessMode = Schema.AccessMode.READ_ONLY)
	private BigDecimal effectiveAmount;

	@Schema(description = "Whether the row is soft-deleted (excluded from the calculation, not resurrected by the daily refresh)", accessMode = Schema.AccessMode.READ_ONLY)
	private boolean deleted;

	@Schema(description = "Free-text note")
	private String note;

	@Schema(description = "When the row was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the row was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static NormExpenseRow create() {
		return new NormExpenseRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormExpenseRow withId(final String id) {
		this.id = id;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormExpenseRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(final String bucket) {
		this.bucket = bucket;
	}

	public NormExpenseRow withBucket(final String bucket) {
		this.bucket = bucket;
		return this;
	}

	public String getCostType() {
		return costType;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public NormExpenseRow withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public NormExpenseRow withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public NormExpenseRow withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public NormExpenseRow withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public BigDecimal getProcessAmount() {
		return processAmount;
	}

	public void setProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
	}

	public NormExpenseRow withProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
		return this;
	}

	public BigDecimal getHandlaggareAmount() {
		return handlaggareAmount;
	}

	public void setHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
	}

	public NormExpenseRow withHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
		return this;
	}

	public BigDecimal getEffectiveAmount() {
		return effectiveAmount;
	}

	public void setEffectiveAmount(final BigDecimal effectiveAmount) {
		this.effectiveAmount = effectiveAmount;
	}

	public NormExpenseRow withEffectiveAmount(final BigDecimal effectiveAmount) {
		this.effectiveAmount = effectiveAmount;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public NormExpenseRow withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormExpenseRow withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NormExpenseRow withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public NormExpenseRow withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormExpenseRow that = (NormExpenseRow) o;
		return deleted == that.deleted && Objects.equals(id, that.id) && Objects.equals(origin, that.origin) && Objects.equals(bucket, that.bucket)
			&& Objects.equals(costType, that.costType) && Objects.equals(otherSubType, that.otherSubType) && Objects.equals(specification, that.specification)
			&& Objects.equals(appliedAmount, that.appliedAmount)
			&& Objects.equals(processAmount, that.processAmount) && Objects.equals(handlaggareAmount, that.handlaggareAmount)
			&& Objects.equals(effectiveAmount, that.effectiveAmount) && Objects.equals(note, that.note) && Objects.equals(created, that.created)
			&& Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, origin, bucket, costType, otherSubType, specification, appliedAmount, processAmount, handlaggareAmount, effectiveAmount, deleted, note,
			created, updated);
	}

	@Override
	public String toString() {
		return "NormExpenseRow{" +
			"id='" + id + '\'' +
			", origin='" + origin + '\'' +
			", bucket='" + bucket + '\'' +
			", costType='" + costType + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", appliedAmount=" + appliedAmount +
			", processAmount=" + processAmount +
			", handlaggareAmount=" + handlaggareAmount +
			", effectiveAmount=" + effectiveAmount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
