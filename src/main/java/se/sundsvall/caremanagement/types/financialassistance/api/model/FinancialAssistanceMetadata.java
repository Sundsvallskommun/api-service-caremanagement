package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * The EB type catalogues the frontend feeds its dropdowns from — the income, cost (boendekostnader) and living-cost
 * (levnadskostnader i övrigt) types, each a {@code code}/Swedish-{@code displayName} pair. The codes are exactly the
 * allowable values of {@code Income.incomeType} and {@code Cost.costType}; cost and living-cost codes share the single
 * {@code Cost.costType} field (the split is the GUI grouping). Served from static constants — no per-municipality
 * configuration.
 */
@Schema(description = "EB type catalogues for the frontend dropdowns: income, cost and living-cost types.")
public class FinancialAssistanceMetadata {

	@Schema(description = "The income types (inkomster)")
	private List<TypeOption> incomeTypes;

	@Schema(description = "The cost types (kostnader — boendekostnader), shown as the first cost dropdown")
	private List<TypeOption> costTypes;

	@Schema(description = "The living-cost types (levnadskostnader i övrigt), shown as the second cost dropdown")
	private List<TypeOption> livingCostTypes;

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

	public List<TypeOption> getLivingCostTypes() {
		return livingCostTypes;
	}

	public void setLivingCostTypes(final List<TypeOption> livingCostTypes) {
		this.livingCostTypes = livingCostTypes;
	}

	public FinancialAssistanceMetadata withLivingCostTypes(final List<TypeOption> livingCostTypes) {
		this.livingCostTypes = livingCostTypes;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinancialAssistanceMetadata that = (FinancialAssistanceMetadata) o;
		return Objects.equals(incomeTypes, that.incomeTypes) && Objects.equals(costTypes, that.costTypes)
			&& Objects.equals(livingCostTypes, that.livingCostTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(incomeTypes, costTypes, livingCostTypes);
	}

	@Override
	public String toString() {
		return "FinancialAssistanceMetadata{incomeTypes=" + incomeTypes + ", costTypes=" + costTypes
			+ ", livingCostTypes=" + livingCostTypes + "}";
	}
}
