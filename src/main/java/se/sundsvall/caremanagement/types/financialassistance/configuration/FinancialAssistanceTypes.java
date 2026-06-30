package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;

/**
 * The complete EB income/cost type catalogue — a label/grouping layer over both surfaces:
 * <ul>
 * <li>the citizen Mina-sidor types ({@code citizenReportable = true}) — keyed on the existing
 * {@code Income.incomeType} / {@code Cost.costType} codes (a guard test asserts the citizen-reportable codes match
 * those
 * models' allowable values), carrying the Mina-sidor label ({@code externalDisplayName}) + form group; and</li>
 * <li>the handläggare-only Lifecare types ({@code citizenReportable = false}) — the rest of the Lifecare
 * normberäkning dropdowns (SSBTEK-derived incomes, El 2, Barnomsorgsavgift, Glasögon, umgänge, Tandvård) that have no
 * Mina-sidor counterpart, carrying only the Lifecare label ({@code internalDisplayName}) and a new code.</li>
 * </ul>
 *
 * <p>
 * Purely additive — this never changes {@code Income}/{@code Cost}. The handläggare-only codes are not added to those
 * models' allowable values; they identify dropdown options for the Draken normberäkning add-row, not the citizen
 * payload. Cost groups are stable codes for the Mina-sidor sections — {@code HOUSING} (Boende),
 * {@code WORK_AND_STUDIES}
 * (Arbete och studier), {@code HEALTH} (Hälsa), {@code OTHER} (Övrigt) — null for income and for handläggare-only
 * types.
 * </p>
 */
public final class FinancialAssistanceTypes {

	private FinancialAssistanceTypes() {}

	// Mina-sidor cost-form section codes (frontend maps to the Swedish headings)
	private static final String GROUP_HOUSING = "HOUSING";
	private static final String GROUP_WORK_AND_STUDIES = "WORK_AND_STUDIES";
	private static final String GROUP_HEALTH = "HEALTH";
	private static final String GROUP_OTHER = "OTHER";

	/** Income types (inkomster) — the citizen Mina-sidor list, then the handläggare-only (SSBTEK-derived) Lifecare list. */
	public static final List<TypeOption> INCOME_TYPES = List.of(
		// Citizen Mina-sidor incomes (the non-SSBTEK ones the applicant reports)
		income("OTHER_INCOME", "Annan inkomst (lån, spelvinst, försörjning av tillgång, gåva, kontanter)", "Övriga inkomster"),
		income("FINANCIAL_AID_OTHER_MUNICIPALITY", "Ekonomiskt bistånd från annan kommun", null),
		income("SALARY", "Lön", "Lön efter skatt"),
		income("SWISH_DEPOSITS", "Swish/kontoinsättningar", "Swish/Insättningar/Överföringar"),
		income("OCCUPATIONAL_PENSION_INSURANCE", "Tjänstepension/försäkringar", null),
		income("CHILD_SUPPORT", "Underhållsbidrag från den andra föräldern", null),
		income("RENT_SHARE_FROM_CHILD", "Hyresdel från barn", null),
		// Handläggare-only Lifecare incomes (no Mina-sidor counterpart — SSBTEK delivers these)
		handlaggareOnly("UNEMPLOYMENT_BENEFIT", "A-kassa"),
		handlaggareOnly("UNEMPLOYMENT_OR_ALPHA_BENEFIT", "A-kassa/Alfaersättning"),
		handlaggareOnly("ACTIVITY_COMPENSATION", "Aktivitetsersättning"),
		handlaggareOnly("ACTIVITY_SUPPORT", "Aktivitetsstöd"),
		handlaggareOnly("ALPHA_BENEFIT", "Alfaersättning"),
		handlaggareOnly("CHILD_ALLOWANCE", "Barnbidrag/Flerbarnstillägg"),
		handlaggareOnly("CHILD_PENSION", "Barnpension"),
		handlaggareOnly("HOUSING_ALLOWANCE", "Bostadsbidrag"),
		handlaggareOnly("HOUSING_SUPPLEMENT", "Bostadstillägg"),
		handlaggareOnly("CSN_GRANT", "CSN Bidrag"),
		handlaggareOnly("CSN_LOAN", "CSN Lån"),
		handlaggareOnly("DAILY_ALLOWANCE_FK", "Dagersättning från FK"),
		handlaggareOnly("SURVIVOR_SUPPORT", "Efterlevandestöd"),
		handlaggareOnly("ESTABLISHMENT_BENEFIT", "Etableringsersättning"),
		handlaggareOnly("PARENTAL_BENEFIT", "Föräldrapenning"),
		handlaggareOnly("LODGING_ALLOWANCE", "Inackorderingstillägg"),
		handlaggareOnly("CAPITAL_INCOME", "Inkomst av kapital"),
		handlaggareOnly("PENSION", "Pension"),
		handlaggareOnly("PENSION_ANNUITY_CARE", "Pension/SA/Livränta/Omvårdnadsbidrag"),
		handlaggareOnly("SICKNESS_COMPENSATION", "Sjukersättning"),
		handlaggareOnly("SICKNESS_BENEFIT", "Sjukpenning"),
		handlaggareOnly("TAX_REFUND", "Skatteåterbäring"),
		handlaggareOnly("MAINTENANCE_SUPPORT", "Underhållsstöd"),
		handlaggareOnly("CARE_ALLOWANCE", "Vårdbidrag/Omvårdnadsbidrag"),
		handlaggareOnly("ELDERLY_SUPPORT", "Äldreförsörjningsstöd"),
		handlaggareOnly("SURPLUS_FROM_PREVIOUS_MONTH", "Överskjutande inkomst från föregående månad"));

	/** Cost types (kostnader) — the citizen Mina-sidor list (grouped), then the handläggare-only Lifecare list. */
	public static final List<TypeOption> COST_TYPES = List.of(
		// Citizen Mina-sidor costs (the "Vilka kostnader söker du bistånd för?" form, grouped)
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
		cost("OTHER", "Övrigt bistånd", "Övriga utgifter", GROUP_OTHER),
		// Handläggare-only Lifecare costs (no Mina-sidor counterpart)
		handlaggareOnly("ELECTRICITY_2", "El 2"),
		handlaggareOnly("CHILDCARE_FEE", "Barnomsorgsavgift"),
		handlaggareOnly("GLASSES", "Glasögon"),
		handlaggareOnly("VISITATION_COST", "Kostnad i samband med umgänge"),
		handlaggareOnly("DENTAL_CARE", "Tandvård"));

	/** The assembled metadata response — the income + cost catalogues the metadata endpoint returns. */
	public static FinancialAssistanceMetadata metadata() {
		return FinancialAssistanceMetadata.create()
			.withIncomeTypes(INCOME_TYPES)
			.withCostTypes(COST_TYPES);
	}

	private static TypeOption income(final String code, final String externalDisplayName, final String internalDisplayName) {
		return option(code, externalDisplayName, internalDisplayName, null, true);
	}

	private static TypeOption cost(final String code, final String externalDisplayName, final String internalDisplayName, final String group) {
		return option(code, externalDisplayName, internalDisplayName, group, true);
	}

	/** A Lifecare type with no Mina-sidor counterpart — internal label only, not on the citizen form. */
	private static TypeOption handlaggareOnly(final String code, final String internalDisplayName) {
		return option(code, null, internalDisplayName, null, false);
	}

	private static TypeOption option(final String code, final String externalDisplayName, final String internalDisplayName,
		final String group, final boolean citizenReportable) {
		return TypeOption.create()
			.withCode(code)
			.withExternalDisplayName(externalDisplayName)
			.withInternalDisplayName(internalDisplayName)
			.withGroup(group)
			.withCitizenReportable(citizenReportable);
	}
}
