package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One selectable financial assistance income/cost type — a label catalogue keyed on the existing
 * {@code Income.incomeType} /
 * {@code Cost.costType} codes, so the same {@code code} carries both presentations:
 * <ul>
 * <li>{@code externalDisplayName} — the citizen Mina-sidor label (e.g. "Hyra (inte parkering/garage)"); null for
 * caseworker-only types not shown on the citizen form.</li>
 * <li>{@code internalDisplayName} — the matching Lifecare caseworker dropdown label (e.g. "Boendekostnad"); null when
 * the type has no Lifecare counterpart.</li>
 * <li>{@code group} — a stable code for the Mina-sidor form section the type is shown under ({@code HOUSING},
 * {@code WORK_AND_STUDIES}, {@code HEALTH}, {@code OTHER}); null for income (a flat list).</li>
 * <li>{@code citizenReportable} — whether the type is offered on the citizen form.</li>
 * </ul>
 *
 * <p>
 * This is purely a label/grouping catalogue served by the metadata endpoint — it never changes the
 * {@code Income.incomeType} / {@code Cost.costType} allowable values (the citizen payload contract).
 * </p>
 */
@Schema(description = "A selectable financial assistance income/cost type — the payload code plus its Mina-sidor + Lifecare labels, form group and citizen flag.")
public class TypeOption {

	@Schema(description = "The type code, as stored on the payload (incomeType / costType)", examples = "RENT")
	private String code;

	@Schema(description = "The citizen Mina-sidor label; null for caseworker-only types not on the citizen form", examples = "Hyra (inte parkering/garage)")
	private String externalDisplayName;

	@Schema(description = "The matching Lifecare caseworker dropdown label, or null when there is no Lifecare counterpart", examples = "Boendekostnad")
	private String internalDisplayName;

	@Schema(description = "Stable code for the Mina-sidor form section the type is shown under; null for income", examples = "HOUSING", allowableValues = {
		"HOUSING", "WORK_AND_STUDIES", "HEALTH", "OTHER"
	})
	private String group;

	@Schema(description = "Whether the type is offered on the citizen Mina-sidor form", examples = "true")
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

	public String getExternalDisplayName() {
		return externalDisplayName;
	}

	public void setExternalDisplayName(final String externalDisplayName) {
		this.externalDisplayName = externalDisplayName;
	}

	public TypeOption withExternalDisplayName(final String externalDisplayName) {
		this.externalDisplayName = externalDisplayName;
		return this;
	}

	public String getInternalDisplayName() {
		return internalDisplayName;
	}

	public void setInternalDisplayName(final String internalDisplayName) {
		this.internalDisplayName = internalDisplayName;
	}

	public TypeOption withInternalDisplayName(final String internalDisplayName) {
		this.internalDisplayName = internalDisplayName;
		return this;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(final String group) {
		this.group = group;
	}

	public TypeOption withGroup(final String group) {
		this.group = group;
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
		return citizenReportable == that.citizenReportable && Objects.equals(code, that.code)
			&& Objects.equals(externalDisplayName, that.externalDisplayName) && Objects.equals(internalDisplayName, that.internalDisplayName)
			&& Objects.equals(group, that.group);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, externalDisplayName, internalDisplayName, group, citizenReportable);
	}

	@Override
	public String toString() {
		return "TypeOption{code='" + code + "', externalDisplayName='" + externalDisplayName + "', internalDisplayName='"
			+ internalDisplayName + "', group='" + group + "', citizenReportable=" + citizenReportable + "}";
	}
}
