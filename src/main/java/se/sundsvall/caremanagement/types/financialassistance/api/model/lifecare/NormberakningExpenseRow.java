package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * An expense row of the normberäkning: an utgift (EXPENSE) or a levnadskostnad i övrigt (SPECIAL_EXPENSE).
 */
@Schema(description = "An expense row of the normberäkning.")
public class NormberakningExpenseRow {

	@Schema(description = "The row id: careM's row id, or the Lifecare bucket and code (E-3, S-7, E-3-2 for a repeated code)")
	private String id;

	@Schema(description = "Stable 0-based position of the row within its section", examples = "0")
	private Integer position;

	@Schema(description = "Who created the row: the process or a caseworker (always CASEWORKER in Lifecare)", allowableValues = {
		"SYSTEM", "CASEWORKER"
	}, accessMode = READ_ONLY)
	private String origin;

	@Schema(description = "Which Lifecare bucket the expense posts to", allowableValues = {
		"EXPENSE", "SPECIAL_EXPENSE"
	})
	private String bucket;

	@Schema(description = "The cost type: careM's code, or Lifecare's expense code")
	private String costType;

	@Schema(description = "The Lifecare label of the cost type", examples = "Boendekostnad")
	private String costTypeDisplayName;

	@Schema(description = "The other sub-type (careM draft only)")
	private String otherSubType;

	@Schema(description = "The cost specification (careM draft only)")
	private String specification;

	@Schema(description = "The amount applied for (ansökt)")
	private BigDecimal appliedAmount;

	@Schema(description = "The amount the rules allowed (careM draft only)")
	private BigDecimal processAmount;

	@Schema(description = "The amount a caseworker decided")
	private BigDecimal caseworkerAmount;

	@Schema(description = "The amount actually used")
	private BigDecimal effectiveAmount;

	@Schema(description = "Whether the row is soft-deleted (careM draft only)")
	private Boolean deleted;

	@Schema(description = "Free-text note")
	private String note;

	public static NormberakningExpenseRow create() {
		return new NormberakningExpenseRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormberakningExpenseRow withId(final String id) {
		this.id = id;
		return this;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}

	public NormberakningExpenseRow withPosition(final Integer position) {
		this.position = position;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormberakningExpenseRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(final String bucket) {
		this.bucket = bucket;
	}

	public NormberakningExpenseRow withBucket(final String bucket) {
		this.bucket = bucket;
		return this;
	}

	public String getCostType() {
		return costType;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public NormberakningExpenseRow withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public String getCostTypeDisplayName() {
		return costTypeDisplayName;
	}

	public void setCostTypeDisplayName(final String costTypeDisplayName) {
		this.costTypeDisplayName = costTypeDisplayName;
	}

	public NormberakningExpenseRow withCostTypeDisplayName(final String costTypeDisplayName) {
		this.costTypeDisplayName = costTypeDisplayName;
		return this;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public NormberakningExpenseRow withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public NormberakningExpenseRow withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public NormberakningExpenseRow withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public BigDecimal getProcessAmount() {
		return processAmount;
	}

	public void setProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
	}

	public NormberakningExpenseRow withProcessAmount(final BigDecimal processAmount) {
		this.processAmount = processAmount;
		return this;
	}

	public BigDecimal getCaseworkerAmount() {
		return caseworkerAmount;
	}

	public void setCaseworkerAmount(final BigDecimal caseworkerAmount) {
		this.caseworkerAmount = caseworkerAmount;
	}

	public NormberakningExpenseRow withCaseworkerAmount(final BigDecimal caseworkerAmount) {
		this.caseworkerAmount = caseworkerAmount;
		return this;
	}

	public BigDecimal getEffectiveAmount() {
		return effectiveAmount;
	}

	public void setEffectiveAmount(final BigDecimal effectiveAmount) {
		this.effectiveAmount = effectiveAmount;
	}

	public NormberakningExpenseRow withEffectiveAmount(final BigDecimal effectiveAmount) {
		this.effectiveAmount = effectiveAmount;
		return this;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(final Boolean deleted) {
		this.deleted = deleted;
	}

	public NormberakningExpenseRow withDeleted(final Boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormberakningExpenseRow withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningExpenseRow that = (NormberakningExpenseRow) o;
		return Objects.equals(id, that.id) && Objects.equals(position, that.position) && Objects.equals(origin, that.origin) && Objects.equals(bucket, that.bucket) && Objects.equals(costType, that.costType) && Objects.equals(costTypeDisplayName,
			that.costTypeDisplayName) && Objects.equals(otherSubType, that.otherSubType) && Objects.equals(specification, that.specification) && Objects.equals(appliedAmount, that.appliedAmount) && Objects.equals(processAmount, that.processAmount)
			&& Objects.equals(caseworkerAmount, that.caseworkerAmount) && Objects.equals(effectiveAmount, that.effectiveAmount) && Objects.equals(deleted, that.deleted) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, position, origin, bucket, costType, costTypeDisplayName, otherSubType, specification, appliedAmount, processAmount, caseworkerAmount, effectiveAmount, deleted, note);
	}

	@Override
	public String toString() {
		return "NormberakningExpenseRow{" +
			"id='" + id + '\'' +
			", position=" + position +
			", origin='" + origin + '\'' +
			", bucket='" + bucket + '\'' +
			", costType='" + costType + '\'' +
			", costTypeDisplayName='" + costTypeDisplayName + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", appliedAmount=" + appliedAmount +
			", processAmount=" + processAmount +
			", caseworkerAmount=" + caseworkerAmount +
			", effectiveAmount=" + effectiveAmount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			'}';
	}
}
