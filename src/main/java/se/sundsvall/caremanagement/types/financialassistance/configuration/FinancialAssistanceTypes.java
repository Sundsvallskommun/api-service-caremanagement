package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;

/**
 * The canonical EB type catalogues — the income, cost (boendekostnader) and living-cost (levnadskostnader i övrigt)
 * types the frontend feeds its dropdowns from, each an English {@code code} paired with the Swedish Lifecare label.
 *
 * <p>
 * These codes are the source of truth for the allowable values of {@code Income.incomeType} and {@code Cost.costType};
 * the validation annotations on those models inline the same literals (a guard test asserts they stay in sync). Cost
 * and
 * living-cost codes share the single {@code Cost.costType} field — the split here is only the GUI grouping (Kostnader
 * vs
 * Levnadskostnader i övrigt), which in turn maps to the FC {@code EXPENSE} / {@code SPECIAL_EXPENSE} buckets the
 * expense
 * regelverk assigns.
 * </p>
 */
public final class FinancialAssistanceTypes {

	private FinancialAssistanceTypes() {}

	/** Income types (inkomster) — the applicant/co-applicant income dropdown. */
	public static final List<TypeOption> INCOME_TYPES = List.of(
		option("UNEMPLOYMENT_BENEFIT", "A-kassa"),
		option("UNEMPLOYMENT_OR_ALPHA_BENEFIT", "A-kassa/Alfaersättning"),
		option("ACTIVITY_COMPENSATION", "Aktivitetsersättning"),
		option("ACTIVITY_SUPPORT", "Aktivitetsstöd"),
		option("ALPHA_BENEFIT", "Alfaersättning"),
		option("CHILD_ALLOWANCE", "Barnbidrag/Flerbarnstillägg"),
		option("CHILD_PENSION", "Barnpension"),
		option("HOUSING_ALLOWANCE", "Bostadsbidrag"),
		option("HOUSING_SUPPLEMENT", "Bostadstillägg"),
		option("CSN_GRANT", "CSN Bidrag"),
		option("CSN_LOAN", "CSN Lån"),
		option("DAILY_ALLOWANCE_FK", "Dagersättning från FK"),
		option("SURVIVOR_SUPPORT", "Efterlevandestöd"),
		option("ESTABLISHMENT_BENEFIT", "Etableringsersättning"),
		option("PARENTAL_BENEFIT", "Föräldrapenning"),
		option("LODGING_ALLOWANCE", "Inackorderingstillägg"),
		option("CAPITAL_INCOME", "Inkomst av kapital"),
		option("SALARY_AFTER_TAX", "Lön efter skatt"),
		option("PENSION", "Pension"),
		option("PENSION_ANNUITY_CARE", "Pension/SA/Livränta/Omvårdnadsbidrag"),
		option("SICKNESS_COMPENSATION", "Sjukersättning"),
		option("SICKNESS_BENEFIT", "Sjukpenning"),
		option("TAX_REFUND", "Skatteåterbäring"),
		option("SWISH_DEPOSITS_TRANSFERS", "Swish/Insättningar/Överföringar"),
		option("MAINTENANCE_SUPPORT", "Underhållsstöd"),
		option("CARE_ALLOWANCE", "Vårdbidrag/Omvårdnadsbidrag"),
		option("ELDERLY_SUPPORT", "Äldreförsörjningsstöd"),
		option("SURPLUS_FROM_PREVIOUS_MONTH", "Överskjutande inkomst från föregående månad"),
		option("OTHER_INCOME", "Övriga inkomster"));

	/** Cost types (kostnader — boendekostnader) — the FC {@code EXPENSE} bucket. */
	public static final List<TypeOption> COST_TYPES = List.of(
		option("UNEMPLOYMENT_FUND_FEE", "A-kasseavgift"),
		option("WORK_TRAVEL", "Arbetsresor"),
		option("HOUSING_COST", "Boendekostnad"),
		option("ELECTRICITY_1", "El 1"),
		option("ELECTRICITY_2", "El 2"),
		option("UNION_FEE", "Fackavgift"),
		option("HOME_INSURANCE", "Hemförsäkring"));

	/** Living-cost types (levnadskostnader i övrigt) — the FC {@code SPECIAL_EXPENSE} bucket. */
	public static final List<TypeOption> LIVING_COST_TYPES = List.of(
		option("CHILDCARE_FEE", "Barnomsorgsavgift"),
		option("BROADBAND_INTERNET", "Bredband/Internet"),
		option("GLASSES", "Glasögon"),
		option("VISITATION_COST", "Kostnad i samband med umgänge"),
		option("MEDICAL_CARE", "Läkarvård"),
		option("MEDICINE", "Medicin"),
		option("DENTAL_CARE", "Tandvård"),
		option("OTHER_EXPENSE", "Övriga utgifter"));

	/** The assembled metadata response — the three catalogues the metadata endpoint returns. */
	public static FinancialAssistanceMetadata metadata() {
		return FinancialAssistanceMetadata.create()
			.withIncomeTypes(INCOME_TYPES)
			.withCostTypes(COST_TYPES)
			.withLivingCostTypes(LIVING_COST_TYPES);
	}

	private static TypeOption option(final String code, final String displayName) {
		return TypeOption.create().withCode(code).withDisplayName(displayName);
	}
}
