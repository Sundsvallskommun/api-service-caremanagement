package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * The financial assistance type catalogue the frontend feeds its dropdowns from — the income and cost types, each
 * carrying its payload
 * code, the Mina-sidor + Lifecare labels, its Mina-sidor form group and the {@code citizenReportable} flag. The codes
 * are exactly the {@code Income.incomeType} / {@code Cost.costType} allowable values; this catalogue only annotates
 * them, it never changes the citizen payload contract.
 */
@Schema(description = "Financial assistance type catalogue for the frontend dropdowns: income and cost types with labels, groups and the citizen flag.")
public class FinancialAssistanceMetadata {

	@Schema(description = "The income types")
	private List<TypeOption> incomeTypes;

	@Schema(description = "The cost types, grouped by their Mina-sidor form section")
	private List<TypeOption> costTypes;

	public static FinancialAssistanceMetadata create() {
		return new FinancialAssistanceMetadata();
	}

	public List<TypeOption> getIncomeTypes() {
		return incomeTypes;
	}

	public void setIncomeTypes(final List<TypeOption> incomeTypes) {
		this.incomeTypes = incomeTypes;
	}

	public FinancialAssistanceMetadata withIncomeTypes(final List<TypeOption> incomeTypes) {
		this.incomeTypes = incomeTypes;
		return this;
	}

	public List<TypeOption> getCostTypes() {
		return costTypes;
	}

	public void setCostTypes(final List<TypeOption> costTypes) {
		this.costTypes = costTypes;
	}

	public FinancialAssistanceMetadata withCostTypes(final List<TypeOption> costTypes) {
		this.costTypes = costTypes;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinancialAssistanceMetadata that = (FinancialAssistanceMetadata) o;
		return Objects.equals(incomeTypes, that.incomeTypes) && Objects.equals(costTypes, that.costTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(incomeTypes, costTypes);
	}

	@Override
	public String toString() {
		return "FinancialAssistanceMetadata{incomeTypes=" + incomeTypes + ", costTypes=" + costTypes + "}";
	}
}
