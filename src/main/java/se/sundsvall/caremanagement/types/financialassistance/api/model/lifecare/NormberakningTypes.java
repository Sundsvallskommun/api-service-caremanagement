package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * The type catalogues the add-row dropdowns offer: Lifecare's own once the beräkning is saved there.
 */
@Schema(description = "The type catalogues of the normberäkning.")
public class NormberakningTypes {

	@ArraySchema(schema = @Schema(description = "Lifecare's norms for the insats; the code is the normId"))
	private List<NormberakningTypeOption> norms;

	@ArraySchema(schema = @Schema(description = "The income types"))
	private List<NormberakningTypeOption> incomeTypes;

	@ArraySchema(schema = @Schema(description = "The utgift types"))
	private List<NormberakningTypeOption> costTypes;

	@ArraySchema(schema = @Schema(description = "The levnadskostnad i övrigt types"))
	private List<NormberakningTypeOption> livingCostTypes;

	public static NormberakningTypes create() {
		return new NormberakningTypes();
	}

	public List<NormberakningTypeOption> getNorms() {
		return norms;
	}

	public void setNorms(final List<NormberakningTypeOption> norms) {
		this.norms = norms;
	}

	public NormberakningTypes withNorms(final List<NormberakningTypeOption> norms) {
		this.norms = norms;
		return this;
	}

	public List<NormberakningTypeOption> getIncomeTypes() {
		return incomeTypes;
	}

	public void setIncomeTypes(final List<NormberakningTypeOption> incomeTypes) {
		this.incomeTypes = incomeTypes;
	}

	public NormberakningTypes withIncomeTypes(final List<NormberakningTypeOption> incomeTypes) {
		this.incomeTypes = incomeTypes;
		return this;
	}

	public List<NormberakningTypeOption> getCostTypes() {
		return costTypes;
	}

	public void setCostTypes(final List<NormberakningTypeOption> costTypes) {
		this.costTypes = costTypes;
	}

	public NormberakningTypes withCostTypes(final List<NormberakningTypeOption> costTypes) {
		this.costTypes = costTypes;
		return this;
	}

	public List<NormberakningTypeOption> getLivingCostTypes() {
		return livingCostTypes;
	}

	public void setLivingCostTypes(final List<NormberakningTypeOption> livingCostTypes) {
		this.livingCostTypes = livingCostTypes;
	}

	public NormberakningTypes withLivingCostTypes(final List<NormberakningTypeOption> livingCostTypes) {
		this.livingCostTypes = livingCostTypes;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningTypes that = (NormberakningTypes) o;
		return Objects.equals(norms, that.norms) && Objects.equals(incomeTypes, that.incomeTypes) && Objects.equals(costTypes, that.costTypes) && Objects.equals(livingCostTypes, that.livingCostTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(norms, incomeTypes, costTypes, livingCostTypes);
	}

	@Override
	public String toString() {
		return "NormberakningTypes{" +
			"norms=" + norms +
			", incomeTypes=" + incomeTypes +
			", costTypes=" + costTypes +
			", livingCostTypes=" + livingCostTypes +
			'}';
	}
}
