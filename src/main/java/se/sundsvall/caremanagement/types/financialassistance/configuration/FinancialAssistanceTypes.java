package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;

/**
 * The EB income/cost type catalogue — a label/grouping layer keyed on the existing {@code Income.incomeType} /
 * {@code Cost.costType} codes (a guard test asserts the codes match those models' allowable values). Each entry pairs
 * the citizen Mina-sidor label ({@code externalDisplayName}) with the Lifecare handläggare-dropdown label
 * ({@code internalDisplayName}), the Mina-sidor form group and the {@code citizenReportable} flag.
 *
 * <p>
 * Purely additive — this never changes {@code Income}/{@code Cost} (the citizen payload contract). The cost groups are
 * stable codes for the Mina-sidor "Vilka kostnader söker du bistånd för?" sections — {@code HOUSING} (Boende),
 * {@code WORK_AND_STUDIES} (Arbete och studier), {@code HEALTH} (Hälsa), {@code OTHER} (Övrigt) — which the frontend
 * maps to the Swedish headings; income is a flat list (no group).
 * </p>
 */
public final class FinancialAssistanceTypes {

	private FinancialAssistanceTypes() {}

	// Mina-sidor cost-form section codes (frontend maps to the Swedish headings)
	private static final String GROUP_HOUSING = "HOUSING";
	private static final String GROUP_WORK_AND_STUDIES = "WORK_AND_STUDIES";
	private static final String GROUP_HEALTH = "HEALTH";
	private static final String GROUP_OTHER = "OTHER";

	/** Income types (inkomster) — a flat Mina-sidor list (no group); all citizen-reportable. */
	public static final List<TypeOption> INCOME_TYPES = List.of(
		income("OTHER_INCOME", "Annan inkomst (lån, spelvinst, försörjning av tillgång, gåva, kontanter)", "Övriga inkomster"),
		income("FINANCIAL_AID_OTHER_MUNICIPALITY", "Ekonomiskt bistånd från annan kommun", null),
		income("SALARY", "Lön", "Lön efter skatt"),
		income("SWISH_DEPOSITS", "Swish/kontoinsättningar", "Swish/Insättningar/Överföringar"),
		income("OCCUPATIONAL_PENSION_INSURANCE", "Tjänstepension/försäkringar (AFA, AMF, KPA, SPV etc)", null),
		income("CHILD_SUPPORT", "Underhållsbidrag från den andra föräldern", null),
		income("RENT_SHARE_FROM_CHILD", "Hyresdel från barn", null));

	/** Cost types (kostnader) — grouped by the Mina-sidor form section; all citizen-reportable. */
	public static final List<TypeOption> COST_TYPES = List.of(
		cost("RENT", "Hyra (inte parkering/garage)", "Boendekostnad", GROUP_HOUSING),
		cost("ELECTRICITY", "Elkostnad (totalsumma)", "El 1", GROUP_HOUSING),
		cost("HOME_INSURANCE", "Hemförsäkring (månadskostnad)", "Hemförsäkring", GROUP_HOUSING),
		cost("INTERNET", "Internet", "Bredband/Internet", GROUP_HOUSING),
		cost("UNEMPLOYMENT_FUND", "A-kassa", "A-kasseavgift", GROUP_WORK_AND_STUDIES),
		cost("UNION_FEE", "Fackföreningsavgift", "Fackavgift", GROUP_WORK_AND_STUDIES),
		cost("TRAVEL_APPROVED", "Resor till godkänd planering/aktivitet", "Arbetsresor", GROUP_WORK_AND_STUDIES),
		cost("TRAVEL_MEDICAL_TRANSPORT", "Resor med sjukresor/färdtjänst till godkänd planering/aktivitet (egenavgift)", "Sjukresor", GROUP_WORK_AND_STUDIES),
		cost("MEDICAL_CARE", "Läkarvård (inom högkostnadsskydd)", "Läkarvård", GROUP_HEALTH),
		cost("MEDICINE", "Medicin (inom högkostnadsskydd/förmån/egenavgift)", "Medicin", GROUP_HEALTH),
		cost("OTHER", "Övrigt bistånd", "Övriga utgifter", GROUP_OTHER));

	/** The assembled metadata response — the income + cost catalogues the metadata endpoint returns. */
	public static FinancialAssistanceMetadata metadata() {
		return FinancialAssistanceMetadata.create()
			.withIncomeTypes(INCOME_TYPES)
			.withCostTypes(COST_TYPES);
	}

	private static TypeOption income(final String code, final String externalDisplayName, final String internalDisplayName) {
		return option(code, externalDisplayName, internalDisplayName, null);
	}

	private static TypeOption cost(final String code, final String externalDisplayName, final String internalDisplayName, final String group) {
		return option(code, externalDisplayName, internalDisplayName, group);
	}

	private static TypeOption option(final String code, final String externalDisplayName, final String internalDisplayName, final String group) {
		return TypeOption.create()
			.withCode(code)
			.withExternalDisplayName(externalDisplayName)
			.withInternalDisplayName(internalDisplayName)
			.withGroup(group)
			.withCitizenReportable(true);
	}
}
