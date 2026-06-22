package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * What a caseworker sends to add a new expense row (origin CASEWORKER) or patch an existing one — only the
 * caseworker
 * amount + note are honoured on a patch.
 */
@Schema(description = "What a caseworker sends to add or patch an expense row (identity + caseworker-writable fields only).")
public class NormExpenseInput {

	@Schema(description = "The cost type")
	private String costType;

	@Schema(description = "Which Lifecare bucket the expense posts to", allowableValues = {
		"EXPENSE", "SPECIAL_EXPENSE"
	})
	private String bucket;

	@Schema(description = "The other sub-type (when the cost type is 'other')")
	private String otherSubType;

	@Schema(description = "The cost specification")
	private String specification;

	@Schema(description = "The amount the caseworker decided", examples = "1100.00")
	private BigDecimal caseworkerAmount;

	@Schema(description = "Free-text note")
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
			&& Objects.equals(specification, that.specification) && Objects.equals(caseworkerAmount, that.caseworkerAmount) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(costType, bucket, otherSubType, specification, caseworkerAmount, note);
	}

	@Override
	public String toString() {
		return "NormExpenseInput{" +
			"costType='" + costType + '\'' +
			", bucket='" + bucket + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", caseworkerAmount=" + caseworkerAmount +
			", note='" + note + '\'' +
			'}';
	}
}
