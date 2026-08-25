package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * What a caseworker sends to add a new expense row (origin CASEWORKER) or patch an existing one — the applied
 * amount, the caseworker amount and the note are all honoured on both create and patch.
 */
@Schema(description = "What a caseworker sends to add or patch an expense row (identity + caseworker-writable fields only).")
public class NormExpenseInput {

	@Schema(description = "The cost type", examples = "Hyra")
	@Size(max = 64)
	private String costType;

	@Schema(description = "Which Lifecare bucket the expense posts to", examples = "EXPENSE", allowableValues = {
		"EXPENSE", "SPECIAL_EXPENSE"
	})
	private String bucket;

	@Schema(description = "The other sub-type (when the cost type is 'other')", examples = "Övrigt")
	@Size(max = 32)
	private String otherSubType;

	@Schema(description = "The cost specification", examples = "Hyra för juni 2026")
	private String specification;

	@Schema(description = "The amount applied for (ansökt). Honoured on both create and patch.", examples = "1100.00")
	private BigDecimal appliedAmount;

	@Schema(description = "The amount the caseworker decided", examples = "1100.00")
	private BigDecimal caseworkerAmount;

	@Schema(description = "Free-text note", examples = "Godkänd enligt underlag")
	private String note;

	public static NormExpenseInput create() {
		return new NormExpenseInput();
	}

	public String getCostType() {
		return costType;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public NormExpenseInput withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(final String bucket) {
		this.bucket = bucket;
	}

	public NormExpenseInput withBucket(final String bucket) {
		this.bucket = bucket;
		return this;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public NormExpenseInput withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public NormExpenseInput withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public NormExpenseInput withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public BigDecimal getCaseworkerAmount() {
		return caseworkerAmount;
	}

	public void setCaseworkerAmount(final BigDecimal caseworkerAmount) {
		this.caseworkerAmount = caseworkerAmount;
	}

	public NormExpenseInput withCaseworkerAmount(final BigDecimal caseworkerAmount) {
		this.caseworkerAmount = caseworkerAmount;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormExpenseInput withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormExpenseInput that = (NormExpenseInput) o;
		return Objects.equals(costType, that.costType) && Objects.equals(bucket, that.bucket) && Objects.equals(otherSubType, that.otherSubType)
			&& Objects.equals(specification, that.specification) && Objects.equals(appliedAmount, that.appliedAmount) && Objects.equals(caseworkerAmount, that.caseworkerAmount)
			&& Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(costType, bucket, otherSubType, specification, appliedAmount, caseworkerAmount, note);
	}

	@Override
	public String toString() {
		return "NormExpenseInput{" +
			"costType='" + costType + '\'' +
			", bucket='" + bucket + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", appliedAmount=" + appliedAmount +
			", caseworkerAmount=" + caseworkerAmount +
			", note='" + note + '\'' +
			'}';
	}
}
