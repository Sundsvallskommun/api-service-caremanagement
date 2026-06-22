package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One selectable type in an EB dropdown — the machine code stored on the payload paired with the Swedish label the
 * frontend shows. Used for the income, cost and living-cost catalogues served by the metadata endpoint, mirroring the
 * {@code code}/{@code displayName} shape of the decision outcomes.
 *
 * <p>
 * {@code citizenReportable} marks whether the type belongs on the citizen Mina-sidor form ({@code true}) or is
 * handläggare-only ({@code false}). The handläggare/normberäkning surface (Draken) sees the whole catalogue; Mina sidor
 * shows only the citizen-reportable types — in practice the income types that do <em>not</em> arrive via SSBTEK (FK,
 * Pensionsmyndigheten, CSN, A-kassa, Skatteverket), since the citizen is told not to re-report those.
 * </p>
 */
@Schema(description = "A selectable EB type — the code stored on the payload, its Swedish label and whether the citizen reports it in Mina sidor.")
public class TypeOption {

	@Schema(description = "The type code, stored on the payload (incomeType / costType)", examples = "HOUSING_COST")
	private String code;

	@Schema(description = "Human-readable Swedish label for the type", examples = "Boendekostnad")
	private String displayName;

	@Schema(description = "Whether the type is shown on the citizen Mina-sidor form (true) or is handläggare-only, e.g. an SSBTEK-sourced income (false)", examples = "true")
	private boolean citizenReportable;

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

	public boolean isCitizenReportable() {
		return citizenReportable;
	}

	public void setCitizenReportable(final boolean citizenReportable) {
		this.citizenReportable = citizenReportable;
	}

	public TypeOption withCitizenReportable(final boolean citizenReportable) {
		this.citizenReportable = citizenReportable;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final TypeOption that = (TypeOption) o;
		return citizenReportable == that.citizenReportable && Objects.equals(code, that.code) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, displayName, citizenReportable);
	}

	@Override
	public String toString() {
		return "TypeOption{code='" + code + "', displayName='" + displayName + "', citizenReportable=" + citizenReportable + "}";
	}
}
