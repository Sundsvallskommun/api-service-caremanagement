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
 * Resolves an EB cost type to the numeric FC expense-type id offered by the calculation proposal — the expense
 * counterpart of {@link ClassifiedIncomeToFcMapper}'s income-type resolution. The bucket selects the proposal
 * catalogue: {@code EXPENSE} → {@code calculationExpenseTypes} (UTGIFTER), {@code SPECIAL_EXPENSE} →
 * {@code calculationSpecialExpenseTypes} (LEVNADSKOSTNADER I ÖVRIGT). The EB→FC name map below is a starting point
 * flagged for the verksamhet to confirm against the real FC catalogues (the agency owns the ids); a cost type that does
 * not resolve is skipped at commit rather than guessed.
 */
public final class ExpenseTypeMapper {

	/** The FC bucket that posts to the special-expense (living costs i övrigt) array. */
	public static final String BUCKET_SPECIAL_EXPENSE = "SPECIAL_EXPENSE";

	/**
	 * EB cost type → the FC expense-type name it is matched against in the proposal — the Lifecare labels of the dropdown
	 * catalogue. The boendekostnader resolve against the regular catalogue, the levnadskostnader i övrigt against the
	 * special catalogue (selected by the bucket). Confirm the exact names with the verksamhet.
	 */
	private static final Map<String, String> FC_NAME_BY_COST_TYPE = Map.ofEntries(
		// Boendekostnader (EXPENSE bucket)
		Map.entry("UNEMPLOYMENT_FUND_FEE", "A-kasseavgift"),
		Map.entry("WORK_TRAVEL", "Arbetsresor"),
		Map.entry("HOUSING_COST", "Boendekostnad"),
		Map.entry("ELECTRICITY_1", "El 1"),
		Map.entry("ELECTRICITY_2", "El 2"),
		Map.entry("UNION_FEE", "Fackavgift"),
		Map.entry("HOME_INSURANCE", "Hemförsäkring"),
		// Levnadskostnader i övrigt (SPECIAL_EXPENSE bucket)
		Map.entry("CHILDCARE_FEE", "Barnomsorgsavgift"),
		Map.entry("BROADBAND_INTERNET", "Bredband/Internet"),
		Map.entry("GLASSES", "Glasögon"),
		Map.entry("VISITATION_COST", "Kostnad i samband med umgänge"),
		Map.entry("MEDICAL_CARE", "Läkarvård"),
		Map.entry("MEDICINE", "Medicin"),
		Map.entry("DENTAL_CARE", "Tandvård"),
		Map.entry("OTHER_EXPENSE", "Övriga utgifter"));

	private ExpenseTypeMapper() {}

	/** Resolve against the regular (UTGIFTER) expense-type catalogue. */
	public static Optional<Integer> resolveExpenseTypeId(final String costType, final PersonBasedCalculationProposalDTO proposal) {
		return resolveExpenseTypeId(costType, proposal, null);
	}

	/**
	 * The FC expense-type id for an EB cost type given the proposal's catalogue for the bucket, or empty when the cost
	 * type is unmapped or the catalogue has no matching name.
	 *
	 * @param  costType the EB cost type (e.g. RENT, MEDICINE)
	 * @param  proposal the FC calculation proposal supplying the type catalogues
	 * @param  bucket   {@code SPECIAL_EXPENSE} to resolve against the special-expense catalogue, else the regular one
	 * @return          the FC type id, or empty
	 */
	public static Optional<Integer> resolveExpenseTypeId(final String costType, final PersonBasedCalculationProposalDTO proposal, final String bucket) {
		final var fcName = FC_NAME_BY_COST_TYPE.get(costType);
		if (fcName == null) {
			return Optional.empty();
		}
		final var catalogue = BUCKET_SPECIAL_EXPENSE.equals(bucket) ? specialIdByName(proposal) : idByName(proposal);
		return ofNullable(catalogue.get(normalize(fcName)));
	}

	private static Map<String, Integer> idByName(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationExpenseTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> normalize(type.getName()), PersonBasedCalculationExpenseTypeDTO::getId, (first, second) -> first));
	}

	private static Map<String, Integer> specialIdByName(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationSpecialExpenseTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> normalize(type.getName()), PersonBasedCalculationSpecialExpenseTypeDTO::getId, (first, second) -> first));
	}

	private static String normalize(final String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}
}
