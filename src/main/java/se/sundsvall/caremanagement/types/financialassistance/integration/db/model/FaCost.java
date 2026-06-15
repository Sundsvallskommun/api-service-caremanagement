package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Objects;

import static org.hibernate.Length.LONG32;

@Embeddable
public class FaCost {

	@Column(name = "cost_type")
	private String costType;

	@Column(name = "applied_amount", precision = 12, scale = 2)
	private BigDecimal appliedAmount;

	@Column(name = "other_sub_type")
	private String otherSubType;

	@Column(name = "specification", length = LONG32)
	private String specification;

	@Column(name = "recipient_or_period", length = LONG32)
	private String recipientOrPeriod;

	public static FaCost create() {
		return new FaCost();
	}

	public String getCostType() {
		return costType;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public String getSpecification() {
		return specification;
	}

	public String getRecipientOrPeriod() {
		return recipientOrPeriod;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public void setRecipientOrPeriod(final String recipientOrPeriod) {
		this.recipientOrPeriod = recipientOrPeriod;
	}

	public FaCost withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public FaCost withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public FaCost withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public FaCost withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public FaCost withRecipientOrPeriod(final String recipientOrPeriod) {
		this.recipientOrPeriod = recipientOrPeriod;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaCost that = (FaCost) o;
		return Objects.equals(costType, that.costType) && Objects.equals(appliedAmount, that.appliedAmount)
			&& Objects.equals(otherSubType, that.otherSubType) && Objects.equals(specification, that.specification)
			&& Objects.equals(recipientOrPeriod, that.recipientOrPeriod);
	}

	@Override
	public int hashCode() {
		return Objects.hash(costType, appliedAmount, otherSubType, specification, recipientOrPeriod);
	}

	@Override
	public String toString() {
		return "FaCost{costType='" + costType + "', appliedAmount=" + appliedAmount + ", otherSubType='" + otherSubType
			+ "', specification='" + specification + "', recipientOrPeriod='" + recipientOrPeriod + "'}";
	}
}
