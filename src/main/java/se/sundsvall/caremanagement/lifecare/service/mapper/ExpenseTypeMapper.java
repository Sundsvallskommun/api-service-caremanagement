package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Resolves an EB cost type to the numeric FC expense-type id offered by the calculation proposal — the expense
 * counterpart of {@link ClassifiedIncomeToFcMapper}'s income-type resolution. The EB→FC name map below is a starting
 * point flagged for the verksamhet to confirm against the real FC expense-type catalogue (the agency owns the ids); a
 * cost type that does not resolve is skipped at commit rather than guessed.
 */
public final class ExpenseTypeMapper {

	/** EB cost type → the FC expense-type name it is matched against in the proposal. Confirm with the verksamhet. */
	private static final Map<String, String> FC_NAME_BY_COST_TYPE = Map.ofEntries(
		Map.entry("RENT", "Hyra"),
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

	private ExpenseTypeMapper() {}

	/**
	 * The FC expense-type id for an EB cost type given the proposal's expense-type catalogue, or empty when the cost type
	 * is unmapped or the catalogue has no matching name.
	 */
	public static Optional<Integer> resolveExpenseTypeId(final String costType, final PersonBasedCalculationProposalDTO proposal) {
		final var fcName = FC_NAME_BY_COST_TYPE.get(costType);
		if (fcName == null) {
			return Optional.empty();
		}
		return ofNullable(idByName(proposal).get(normalize(fcName)));
	}

	private static Map<String, Integer> idByName(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationExpenseTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> normalize(type.getName()), PersonBasedCalculationExpenseTypeDTO::getId, (first, second) -> first));
	}

	private static String normalize(final String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}
}
