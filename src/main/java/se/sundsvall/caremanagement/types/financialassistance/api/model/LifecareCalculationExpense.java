package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single expense row on a Lifecare calculation — used for both regular and special expenses — with the applied
 * amount and the amount Lifecare approved.
 */
@Schema(description = "An expense row on a Lifecare calculation.")
public class LifecareCalculationExpense {

	@Schema(description = "The expense type", examples = "Hyra")
	private String type;

	@Schema(description = "The applied amount", examples = "7500.0")
	private BigDecimal appliedAmount;

	@Schema(description = "The approved amount", examples = "7000.0")
	private BigDecimal approvedAmount;

	public static LifecareCalculationExpense create() {
		return new LifecareCalculationExpense();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public LifecareCalculationExpense withType(final String type) {
		this.type = type;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public LifecareCalculationExpense withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public BigDecimal getApprovedAmount() {
		return approvedAmount;
	}

	public void setApprovedAmount(final BigDecimal approvedAmount) {
		this.approvedAmount = approvedAmount;
	}

	public LifecareCalculationExpense withApprovedAmount(final BigDecimal approvedAmount) {
		this.approvedAmount = approvedAmount;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareCalculationExpense that = (LifecareCalculationExpense) o;
		return Objects.equals(type, that.type) && Objects.equals(appliedAmount, that.appliedAmount) && Objects.equals(approvedAmount, that.approvedAmount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, appliedAmount, approvedAmount);
	}

	@Override
	public String toString() {
		return "LifecareCalculationExpense{type='" + type + "', appliedAmount=" + appliedAmount + ", approvedAmount=" + approvedAmount + "}";
	}
}
