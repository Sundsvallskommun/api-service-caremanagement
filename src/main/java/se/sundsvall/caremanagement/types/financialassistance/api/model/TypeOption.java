package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One selectable type in an EB dropdown — the machine code stored on the payload paired with the Swedish label the
 * frontend shows. Used for the income, cost and living-cost catalogues served by the metadata endpoint, mirroring the
 * {@code code}/{@code displayName} shape of the decision outcomes. The {@code code} is the value the frontend sends
 * back
 * on {@code Income.incomeType} / {@code Cost.costType}.
 */
@Schema(description = "A selectable EB type — the code stored on the payload plus its Swedish display label.")
public class TypeOption {

	@Schema(description = "The type code, stored on the payload (incomeType / costType)", examples = "HOUSING_COST")
	private String code;

	@Schema(description = "Human-readable Swedish label for the type", examples = "Boendekostnad")
	private String displayName;

	public static TypeOption create() {
		return new TypeOption();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public TypeOption withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public TypeOption withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final TypeOption that = (TypeOption) o;
		return Objects.equals(code, that.code) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, displayName);
	}

	@Override
	public String toString() {
		return "TypeOption{code='" + code + "', displayName='" + displayName + "'}";
	}
}
