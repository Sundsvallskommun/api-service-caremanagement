package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * What a handläggare sends to add a new expense row (origin HANDLAGGARE) or patch an existing one — only the
 * handläggare
 * amount + note are honoured on a patch.
 */
@Schema(description = "What a handläggare sends to add or patch an expense row (identity + handläggare-writable fields only).")
public class NormExpenseInput {

	@Schema(description = "The cost type")
	private String costType;

	@Schema(description = "The other sub-type (when the cost type is 'other')")
	private String otherSubType;

	@Schema(description = "The cost specification")
	private String specification;

	@Schema(description = "The amount the handläggare decided", examples = "1100.00")
	private BigDecimal handlaggareAmount;

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

	public BigDecimal getHandlaggareAmount() {
		return handlaggareAmount;
	}

	public void setHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
	}

	public NormExpenseInput withHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
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
		return Objects.equals(costType, that.costType) && Objects.equals(otherSubType, that.otherSubType) && Objects.equals(specification, that.specification)
			&& Objects.equals(handlaggareAmount, that.handlaggareAmount) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(costType, otherSubType, specification, handlaggareAmount, note);
	}

	@Override
	public String toString() {
		return "NormExpenseInput{" +
			"costType='" + costType + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", handlaggareAmount=" + handlaggareAmount +
			", note='" + note + '\'' +
			'}';
	}
}
