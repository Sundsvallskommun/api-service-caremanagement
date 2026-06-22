package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "A cost the applicant is applying for assistance with.")
public class Cost {

	@Schema(description = "The type of cost — boendekostnader (EXPENSE) and levnadskostnader i övrigt (SPECIAL_EXPENSE); see GET .../errands/financial-assistance/metadata for the labelled catalogue", examples = "HOUSING_COST", allowableValues = {
		"UNEMPLOYMENT_FUND_FEE", "WORK_TRAVEL", "HOUSING_COST", "ELECTRICITY_1", "ELECTRICITY_2", "UNION_FEE", "HOME_INSURANCE", "CHILDCARE_FEE", "BROADBAND_INTERNET", "GLASSES", "VISITATION_COST", "MEDICAL_CARE", "MEDICINE", "DENTAL_CARE", "OTHER_EXPENSE"
	})
	@OneOf(value = {
		"UNEMPLOYMENT_FUND_FEE", "WORK_TRAVEL", "HOUSING_COST", "ELECTRICITY_1", "ELECTRICITY_2", "UNION_FEE", "HOME_INSURANCE", "CHILDCARE_FEE", "BROADBAND_INTERNET", "GLASSES", "VISITATION_COST", "MEDICAL_CARE", "MEDICINE", "DENTAL_CARE", "OTHER_EXPENSE"
	}, nullable = true)
	private String costType;

	@Schema(description = "The amount applied for", examples = "5400.00")
	private BigDecimal appliedAmount;

	@Schema(description = "Sub type when the cost type is OTHER", examples = "MUNICIPAL_FEES", allowableValues = {
		"OTHER", "MUNICIPAL_FEES", "ACUTE_DENTAL"
	})
	@OneOf(value = {
		"OTHER", "MUNICIPAL_FEES", "ACUTE_DENTAL"
	}, nullable = true)
	private String otherSubType;

	@Schema(description = "Free text specification of the cost", examples = "Dental care at the public dental service")
	private String specification;

	@Schema(description = "Recipient of the cost or the period it covers", examples = "Mitt Bostads AB, juni 2026")
	private String recipientOrPeriod;

	public static Cost create() {
		return new Cost();
	}

	public String getCostType() {
		return costType;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public Cost withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public Cost withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public Cost withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public Cost withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public String getRecipientOrPeriod() {
		return recipientOrPeriod;
	}

	public void setRecipientOrPeriod(final String recipientOrPeriod) {
		this.recipientOrPeriod = recipientOrPeriod;
	}

	public Cost withRecipientOrPeriod(final String recipientOrPeriod) {
		this.recipientOrPeriod = recipientOrPeriod;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Cost that = (Cost) o;
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
		return "Cost{costType='" + costType + "', appliedAmount=" + appliedAmount + ", otherSubType='" + otherSubType
			+ "', specification='" + specification + "', recipientOrPeriod='" + recipientOrPeriod + "'}";
	}
}
