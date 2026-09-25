package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * An expense row of a previous Lifecare beräkning.
 */
@Schema(description = "An expense row of a previous Lifecare beräkning.")
public class NormberakningPreviousExpense {

	@Schema(description = "The expense type", examples = "Boendekostnad")
	private String type;

	@Schema(description = "The applied amount")
	private BigDecimal appliedAmount;

	@Schema(description = "The approved amount")
	private BigDecimal approvedAmount;

	public static NormberakningPreviousExpense create() {
		return new NormberakningPreviousExpense();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public NormberakningPreviousExpense withType(final String type) {
		this.type = type;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public NormberakningPreviousExpense withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public BigDecimal getApprovedAmount() {
		return approvedAmount;
	}

	public void setApprovedAmount(final BigDecimal approvedAmount) {
		this.approvedAmount = approvedAmount;
	}

	public NormberakningPreviousExpense withApprovedAmount(final BigDecimal approvedAmount) {
		this.approvedAmount = approvedAmount;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningPreviousExpense that = (NormberakningPreviousExpense) o;
		return Objects.equals(type, that.type) && Objects.equals(appliedAmount, that.appliedAmount) && Objects.equals(approvedAmount, that.approvedAmount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, appliedAmount, approvedAmount);
	}

	@Override
	public String toString() {
		return "NormberakningPreviousExpense{" +
			"type='" + type + '\'' +
			", appliedAmount=" + appliedAmount +
			", approvedAmount=" + approvedAmount +
			'}';
	}
}
