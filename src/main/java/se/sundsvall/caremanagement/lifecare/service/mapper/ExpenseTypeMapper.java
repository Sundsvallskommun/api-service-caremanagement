package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpenseTypeDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Resolves a financial assistance cost type to the numeric FamilyCare expense-type id offered by the calculation
 * proposal — the expense counterpart of {@link ClassifiedIncomeToFamilyCareMapper}'s income-type resolution. The
 * bucket selects the proposal catalogue: {@code EXPENSE} → {@code calculationExpenseTypes} (UTGIFTER), {@code
 * SPECIAL_EXPENSE} → {@code calculationSpecialExpenseTypes} (LEVNADSKOSTNADER I ÖVRIGT). The financial
 * assistance→FamilyCare name map below is a starting point flagged for the verksamhet to confirm against the real
 * FamilyCare catalogues (the agency owns the ids); a cost type that does not resolve is skipped at commit rather than
 * guessed.
 */
public final class ExpenseTypeMapper {

	/** The FamilyCare bucket that posts to the special-expense (living costs i övrigt) array. */
	public static final String BUCKET_SPECIAL_EXPENSE = "SPECIAL_EXPENSE";

	/**
	 * Financial assistance cost type → the FamilyCare expense-type name matched in the proposal. Confirm with the
	 * verksamhet.
	 */
	private static final Map<String, String> FAMILYCARE_NAME_BY_COST_TYPE = Map.ofEntries(
		Map.entry("RENT", "Rent"),
		Map.entry("ELECTRICITY", "El"),
		Map.entry("HOME_INSURANCE", "Hemförsäkring"),
		Map.entry("INTERNET", "Bredband"),
		Map.entry("UNEMPLOYMENT_FUND", "A-kassa"),
		Map.entry("UNION_FEE", "Fackavgift"),
		Map.entry("TRAVEL_APPROVED", "Resor"),
		Map.entry("TRAVEL_MEDICAL_TRANSPORT", "Sjukresor"),
		Map.entry("MEDICAL_CARE", "Läkarvård"),
		Map.entry("MEDICINE", "Medicin"),
		Map.entry("OTHER", "Övrigt"));

	/**
	 * FamilyCare expense-type name (normalized, i.e. as {@link MapperUtil#normalize}) → financial assistance cost type, for
	 * reading a previous calculation's amounts back per cost type. Spelled out rather than derived so both directions read
	 * the same way; {@code ExpenseTypeMapperTest} asserts the two maps stay exact inverses, so adding a cost type to one
	 * and not the other fails the build rather than silently dropping an amount.
	 */
	private static final Map<String, String> COST_TYPE_BY_FAMILYCARE_NAME = Map.ofEntries(
		Map.entry("rent", "RENT"),
		Map.entry("el", "ELECTRICITY"),
		Map.entry("hemförsäkring", "HOME_INSURANCE"),
		Map.entry("bredband", "INTERNET"),
		Map.entry("a-kassa", "UNEMPLOYMENT_FUND"),
		Map.entry("fackavgift", "UNION_FEE"),
		Map.entry("resor", "TRAVEL_APPROVED"),
		Map.entry("sjukresor", "TRAVEL_MEDICAL_TRANSPORT"),
		Map.entry("läkarvård", "MEDICAL_CARE"),
		Map.entry("medicin", "MEDICINE"),
		Map.entry("övrigt", "OTHER"));

	private ExpenseTypeMapper() {}

	/** The forward mapping, exposed so the test can assert the two directions stay exact inverses. */
	static Map<String, String> familyCareNameByCostType() {
		return FAMILYCARE_NAME_BY_COST_TYPE;
	}

	/** The reverse mapping, exposed so the test can assert the two directions stay exact inverses. */
	static Map<String, String> costTypeByFamilyCareName() {
		return COST_TYPE_BY_FAMILYCARE_NAME;
	}

	/**
	 * The financial assistance cost type for a FamilyCare expense-type name (e.g. "Rent" → {@code RENT}), or empty when
	 * the name is unmapped. Best-effort, case/space-insensitive — used to read a previous Lifecare calculation's
	 * per-type approved amounts.
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
