package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpenseTypeDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Resolves a financial assistance cost type to the numeric FamilyCare expense-type id offered by the calculation
 * proposal — the expense counterpart of {@link ClassifiedIncomeToFamilyCareMapper}'s income-type resolution. The
 * bucket selects the proposal catalogue: {@code EXPENSE} → {@code calculationExpenseTypes} (UTGIFTER), {@code
 * SPECIAL_EXPENSE} → {@code calculationSpecialExpenseTypes} (LEVNADSKOSTNADER I ÖVRIGT). A cost type that does not
 * resolve is skipped at commit rather than guessed.
 *
 * <p>
 * The names are the Lifecare caseworker dropdown labels, verified against a live FamilyCare
 * {@code Calculations/Proposals} response on 2026-09-18. They previously read {@code "Rent"}, {@code "El"},
 * {@code "Bredband"}, {@code "A-kassa"}, {@code "Resor"} and {@code "Övrigt"} — guesses, none of which exists in the
 * catalogue. Every one resolved to no id, so the cost was silently dropped from the normberäkning, boendekostnaden
 * included. {@code ExpenseTypeMapperTest} now asserts this table agrees with
 * {@code FinancialAssistanceTypes.COST_TYPES} entry for entry, so the two cannot drift apart again; the check lives
 * in the test rather than in a derivation here because {@code types.financialassistance} already depends on
 * {@code lifecare}, and reading its catalogue from here would close a module cycle.
 * </p>
 *
 * <p>
 * {@code TRAVEL_MEDICAL_TRANSPORT} ("Sjukresor") is deliberately left pointing at a name the catalogue does not
 * offer, in either the expense or the special-expense list: FamilyCare has no sjukresor row, so the cost has nowhere
 * to post. It keeps being skipped at commit until the verksamhet says which row it belongs in.
 * </p>
 */
public final class ExpenseTypeMapper {

	/** The FamilyCare bucket that posts to the special-expense (living costs i övrigt) array. */
	public static final String BUCKET_SPECIAL_EXPENSE = "SPECIAL_EXPENSE";

	/** Financial assistance cost type → the FamilyCare expense-type name matched in the proposal. */
	private static final Map<String, String> FAMILYCARE_NAME_BY_COST_TYPE = Map.ofEntries(
		Map.entry("RENT", "Boendekostnad"),
		Map.entry("ELECTRICITY", "El 1"),
		Map.entry("ELECTRICITY_2", "El 2"),
		Map.entry("HOME_INSURANCE", "Hemförsäkring"),
		Map.entry("INTERNET", "Bredband/Internet"),
		Map.entry("UNEMPLOYMENT_FUND", "A-kasseavgift"),
		Map.entry("UNION_FEE", "Fackavgift"),
		Map.entry("TRAVEL_APPROVED", "Arbetsresor"),
		Map.entry("TRAVEL_MEDICAL_TRANSPORT", "Sjukresor"),
		Map.entry("MEDICAL_CARE", "Läkarvård"),
		Map.entry("MEDICINE", "Medicin"),
		Map.entry("CHILDCARE_FEE", "Barnomsorgsavgift"),
		Map.entry("GLASSES", "Glasögon"),
		Map.entry("VISITATION_COST", "Kostnad i samband med umgänge"),
		Map.entry("DENTAL_CARE", "Tandvård"),
		Map.entry("OTHER", "Övriga utgifter"));

	/**
	 * FamilyCare expense-type name (normalized, i.e. as {@link MapperUtil#normalize}) → financial assistance cost type,
	 * for reading a previous calculation's amounts back per cost type. Inverse of the map above by construction;
	 * {@code ExpenseTypeMapperTest} still asserts the two stay exact inverses.
	 */
	private static final Map<String, String> COST_TYPE_BY_FAMILYCARE_NAME = invert(FAMILYCARE_NAME_BY_COST_TYPE);

	private ExpenseTypeMapper() {}

	private static Map<String, String> invert(final Map<String, String> nameByCostType) {
		return unmodifiableMap(nameByCostType.entrySet().stream()
			.collect(toMap(entry -> MapperUtil.normalize(entry.getValue()), Map.Entry::getKey, (first, second) -> first, LinkedHashMap::new)));
	}

	/** The forward mapping, exposed so the test can assert the two directions stay exact inverses. */
	static Map<String, String> familyCareNameByCostType() {
		return FAMILYCARE_NAME_BY_COST_TYPE;
	}

	/** The reverse mapping, exposed so the test can assert the two directions stay exact inverses. */
	static Map<String, String> costTypeByFamilyCareName() {
		return COST_TYPE_BY_FAMILYCARE_NAME;
	}

	/**
	 * The financial assistance cost type for a FamilyCare expense-type name (e.g. "Boendekostnad" → {@code RENT}), or
	 * empty when the name is unmapped. Best-effort, case/space-insensitive — used to read a previous Lifecare
	 * calculation's per-type approved amounts.
	 */
	public static Optional<String> costTypeForFamilyCareName(final String familyCareName) {
		return ofNullable(familyCareName).map(MapperUtil::normalize).map(COST_TYPE_BY_FAMILYCARE_NAME::get);
	}

	/**
	 * The FamilyCare expense-type id for a financial assistance cost type given the proposal's catalogue for the bucket,
	 * or empty when the cost type is unmapped or the catalogue has no matching name.
	 *
	 * @param  costType the financial assistance cost type (e.g. RENT, MEDICINE)
	 * @param  proposal the FamilyCare calculation proposal supplying the type catalogues
	 * @param  bucket   {@code SPECIAL_EXPENSE} to resolve against the special-expense catalogue, else the regular one
	 * @return          the FamilyCare type id, or empty
	 */
	public static Optional<Integer> resolveExpenseTypeId(final String costType, final PersonBasedCalculationProposalDTO proposal, final String bucket) {
		final var familyCareName = FAMILYCARE_NAME_BY_COST_TYPE.get(costType);
		if (familyCareName == null) {
			return Optional.empty();
		}
		final Map<String, Integer> catalogue;
		if (BUCKET_SPECIAL_EXPENSE.equals(bucket)) {
			catalogue = specialIdByName(proposal);
		} else {
			catalogue = idByName(proposal);
		}
		return ofNullable(catalogue.get(MapperUtil.normalize(familyCareName)));
	}

	private static Map<String, Integer> idByName(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationExpenseTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> MapperUtil.normalize(type.getName()), PersonBasedCalculationExpenseTypeDTO::getId, (first, second) -> first));
	}

	private static Map<String, Integer> specialIdByName(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationSpecialExpenseTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> MapperUtil.normalize(type.getName()), PersonBasedCalculationSpecialExpenseTypeDTO::getId, (first, second) -> first));
	}
}
