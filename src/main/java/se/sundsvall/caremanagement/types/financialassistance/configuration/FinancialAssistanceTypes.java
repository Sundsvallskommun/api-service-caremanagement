package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;

/**
 * The canonical EB type catalogues — the income, cost (boendekostnader) and living-cost (levnadskostnader i övrigt)
 * types, each an English {@code code} paired with the Swedish Lifecare label and a {@code citizenReportable} flag.
 *
 * <p>
 * This is the <em>complete</em> handläggare/normberäkning set (Draken sees all of it, and {@code Income.incomeType} /
 * {@code Cost.costType} validate against it — a guard test keeps the codes in sync). {@code citizenReportable} marks
 * the
 * subset the citizen Mina-sidor form offers: the income types that do <em>not</em> arrive via SSBTEK (FK,
 * Pensionsmyndigheten, CSN, A-kassa, Skatteverket) are reportable; the SSBTEK-sourced ones are handläggare-only, since
 * the citizen is told not to re-report them. All costs are citizen-reportable (costs are not SSBTEK-sourced).
 * </p>
 *
 * <p>
 * Cost and living-cost codes share the single {@code Cost.costType} field — the split is the GUI grouping (Kostnader vs
 * Levnadskostnader i övrigt), which maps to the FC {@code EXPENSE} / {@code SPECIAL_EXPENSE} buckets the expense
 * regelverk assigns.
 * </p>
 */
public final class FinancialAssistanceTypes {

	private FinancialAssistanceTypes() {}

	/** Income types (inkomster). Citizen-reportable = the non-SSBTEK incomes the applicant enters in Mina sidor. */
	public static final List<TypeOption> INCOME_TYPES = List.of(
		option("UNEMPLOYMENT_BENEFIT", "A-kassa", false),
		option("UNEMPLOYMENT_OR_ALPHA_BENEFIT", "A-kassa/Alfaersättning", false),
		option("ACTIVITY_COMPENSATION", "Aktivitetsersättning", false),
		option("ACTIVITY_SUPPORT", "Aktivitetsstöd", false),
		option("ALPHA_BENEFIT", "Alfaersättning", false),
		option("CHILD_ALLOWANCE", "Barnbidrag/Flerbarnstillägg", false),
		option("CHILD_PENSION", "Barnpension", false),
		option("HOUSING_ALLOWANCE", "Bostadsbidrag", false),
		option("HOUSING_SUPPLEMENT", "Bostadstillägg", false),
		option("CSN_GRANT", "CSN Bidrag", false),
		option("CSN_LOAN", "CSN Lån", false),
		option("DAILY_ALLOWANCE_FK", "Dagersättning från FK", false),
		option("SURVIVOR_SUPPORT", "Efterlevandestöd", false),
		option("FINANCIAL_AID_OTHER_MUNICIPALITY", "Ekonomiskt bistånd från annan kommun", true),
		option("ESTABLISHMENT_BENEFIT", "Etableringsersättning", false),
		option("PARENTAL_BENEFIT", "Föräldrapenning", false),
		option("RENT_SHARE_FROM_CHILD", "Hyresdel från barn", true),
		option("LODGING_ALLOWANCE", "Inackorderingstillägg", false),
		option("CAPITAL_INCOME", "Inkomst av kapital", false),
		option("SALARY_AFTER_TAX", "Lön efter skatt", true),
		option("PENSION", "Pension", false),
		option("PENSION_ANNUITY_CARE", "Pension/SA/Livränta/Omvårdnadsbidrag", false),
		option("SICKNESS_COMPENSATION", "Sjukersättning", false),
		option("SICKNESS_BENEFIT", "Sjukpenning", false),
		option("TAX_REFUND", "Skatteåterbäring", false),
		option("SWISH_DEPOSITS_TRANSFERS", "Swish/Insättningar/Överföringar", true),
		option("OCCUPATIONAL_PENSION_INSURANCE", "Tjänstepension/försäkringar", true),
		option("CHILD_SUPPORT", "Underhållsbidrag från den andra föräldern", true),
		option("MAINTENANCE_SUPPORT", "Underhållsstöd", false),
		option("CARE_ALLOWANCE", "Vårdbidrag/Omvårdnadsbidrag", false),
		option("ELDERLY_SUPPORT", "Äldreförsörjningsstöd", false),
		option("SURPLUS_FROM_PREVIOUS_MONTH", "Överskjutande inkomst från föregående månad", false),
		option("OTHER_INCOME", "Övriga inkomster", true));

	/** Cost types (kostnader — boendekostnader) — the FC {@code EXPENSE} bucket. All citizen-reportable. */
	public static final List<TypeOption> COST_TYPES = List.of(
		option("UNEMPLOYMENT_FUND_FEE", "A-kasseavgift", true),
		option("WORK_TRAVEL", "Arbetsresor", true),
		option("HOUSING_COST", "Boendekostnad", true),
		option("ELECTRICITY_1", "El 1", true),
		option("ELECTRICITY_2", "El 2", true),
		option("UNION_FEE", "Fackavgift", true),
		option("HOME_INSURANCE", "Hemförsäkring", true));

	/** Living-cost types (levnadskostnader i övrigt) — the FC {@code SPECIAL_EXPENSE} bucket. All citizen-reportable. */
	public static final List<TypeOption> LIVING_COST_TYPES = List.of(
		option("CHILDCARE_FEE", "Barnomsorgsavgift", true),
		option("BROADBAND_INTERNET", "Bredband/Internet", true),
		option("GLASSES", "Glasögon", true),
		option("VISITATION_COST", "Kostnad i samband med umgänge", true),
		option("MEDICAL_CARE", "Läkarvård", true),
		option("MEDICINE", "Medicin", true),
		option("DENTAL_CARE", "Tandvård", true),
		option("OTHER_EXPENSE", "Övriga utgifter", true));

	/** The assembled metadata response — the three catalogues the metadata endpoint returns. */
	public static FinancialAssistanceMetadata metadata() {
		return FinancialAssistanceMetadata.create()
			.withIncomeTypes(INCOME_TYPES)
			.withCostTypes(COST_TYPES)
			.withLivingCostTypes(LIVING_COST_TYPES);
	}

	private static TypeOption option(final String code, final String displayName, final boolean citizenReportable) {
		return TypeOption.create().withCode(code).withDisplayName(displayName).withCitizenReportable(citizenReportable);
	}
}
