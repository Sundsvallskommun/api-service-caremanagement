package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationSpecialExpenseTypeDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Resolves a financial assistance cost type to the numeric FC expense-type id offered by the calculation proposal — the
 * expense
 * counterpart of {@link ClassifiedIncomeToFcMapper}'s income-type resolution. The bucket selects the proposal
 * catalogue: {@code EXPENSE} → {@code calculationExpenseTypes} (UTGIFTER), {@code SPECIAL_EXPENSE} →
 * {@code calculationSpecialExpenseTypes} (LEVNADSKOSTNADER I ÖVRIGT). The financial assistance→FC name map below is a
 * starting point
 * flagged for the verksamhet to confirm against the real FC catalogues (the agency owns the ids); a cost type that does
 * not resolve is skipped at commit rather than guessed.
 */
public final class ExpenseTypeMapper {

	/** The FC bucket that posts to the special-expense (living costs i övrigt) array. */
	public static final String BUCKET_SPECIAL_EXPENSE = "SPECIAL_EXPENSE";

	/**
	 * financial assistance cost type → the FC expense-type name it is matched against in the proposal. Confirm with the
	 * verksamhet.
	 */
	private static final Map<String, String> FC_NAME_BY_COST_TYPE = Map.ofEntries(
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
	 * FC expense-type name (normalized) → financial assistance cost type — the reverse of {@link #FC_NAME_BY_COST_TYPE},
	 * for reading a
	 * previous calculation's amounts back per cost type.
	 */
	private static final Map<String, String> COST_TYPE_BY_FC_NAME = FC_NAME_BY_COST_TYPE.entrySet().stream()
		.collect(toMap(entry -> MapperUtil.normalize(entry.getValue()), Map.Entry::getKey, (first, second) -> first));

	private ExpenseTypeMapper() {}

	/**
	 * The financial assistance cost type for an FC expense-type name (e.g. "Rent" → {@code RENT}), or empty when the name
	 * is unmapped.
	 * Best-effort, case/space-insensitive — used to read a previous Lifecare calculation's per-type approved amounts.
	 */
	public static Optional<String> costTypeForFcName(final String fcName) {
		return ofNullable(fcName).map(MapperUtil::normalize).map(COST_TYPE_BY_FC_NAME::get);
	}

	/**
	 * The FC expense-type id for a financial assistance cost type given the proposal's catalogue for the bucket, or empty
	 * when the cost
	 * type is unmapped or the catalogue has no matching name.
	 *
	 * @param  costType the financial assistance cost type (e.g. RENT, MEDICINE)
	 * @param  proposal the FC calculation proposal supplying the type catalogues
	 * @param  bucket   {@code SPECIAL_EXPENSE} to resolve against the special-expense catalogue, else the regular one
	 * @return          the FC type id, or empty
	 */
	public static Optional<Integer> resolveExpenseTypeId(final String costType, final PersonBasedCalculationProposalDTO proposal, final String bucket) {
		final var fcName = FC_NAME_BY_COST_TYPE.get(costType);
		if (fcName == null) {
			return Optional.empty();
		}
		final Map<String, Integer> catalogue;
		if (BUCKET_SPECIAL_EXPENSE.equals(bucket)) {
			catalogue = specialIdByName(proposal);
		} else {
			catalogue = idByName(proposal);
		}
		return ofNullable(catalogue.get(MapperUtil.normalize(fcName)));
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
