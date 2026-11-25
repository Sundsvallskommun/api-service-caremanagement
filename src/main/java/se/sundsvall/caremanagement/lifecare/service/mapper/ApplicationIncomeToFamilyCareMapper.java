package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncomeLines;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Maps the incomes the applicant declared in the application to FamilyCare calculation income lines — the application
 * side of {@link ClassifiedIncomeToFamilyCareMapper}. Each application income code is resolved to its FamilyCare
 * income-type name through {@link IncomeTypeMapper#APPLICATION_TYPE_TO_FC_NAME}, and that name to the numeric type id
 * the applicant's calculation proposal offers. Incomes of the same type and recipient are summed into one line.
 */
public final class ApplicationIncomeToFamilyCareMapper {

	static final String RECIPIENT_APPLICANT = "APPLICANT";
	static final String RECIPIENT_CO_APPLICANT = "CO_APPLICANT";
	static final String NOTE_PREFIX = "Ansökan: ";

	private ApplicationIncomeToFamilyCareMapper() {}

	/**
	 * @param  incomes  the incomes declared in the application (maybe {@code null})
	 * @param  proposal the FamilyCare proposal whose {@code calculationIncomeTypes} supply the type ids and names
	 * @return          the resolved lines, and the declared incomes no type in the proposal could take
	 */
	public static ApplicationIncomeLines toIncomeLines(final List<ApplicationIncome> incomes, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = MapperUtil.indexIncomeTypeIds(proposal);
		final var nameById = indexIncomeTypeNamesById(proposal);

		final var groups = new LinkedHashMap<String, List<Resolved>>();
		final var untransferable = new ArrayList<ApplicationIncome>();
		ofNullable(incomes).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(income -> hasAmount(income.amount()))
			.forEach(income -> ofNullable(IncomeTypeMapper.APPLICATION_TYPE_TO_FC_NAME.get(income.incomeType()))
				.map(MapperUtil::normalize)
				.map(typeIdByName::get)
				.ifPresentOrElse(
					typeId -> groups.computeIfAbsent(typeId + "|" + recipient(income), _ -> new ArrayList<>()).add(new Resolved(typeId, income)),
					() -> untransferable.add(income)));

		final var lines = groups.values().stream()
			.map(group -> toLine(group, nameById))
			.toList();
		return new ApplicationIncomeLines(lines, List.copyOf(untransferable));
	}

	private static FamilyCareIncomeLine toLine(final List<Resolved> group, final Map<Integer, String> nameById) {
		final var typeId = group.getFirst().typeId();
		final var amount = group.stream().map(resolved -> resolved.income().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
		final var latest = group.stream()
			.map(resolved -> resolved.income().date())
			.filter(Objects::nonNull)
			.max(Comparator.naturalOrder())
			.orElse(null);
		return new FamilyCareIncomeLine(typeId, nameById.get(typeId), recipient(group.getFirst().income()), amount, MapperUtil.toOffsetDateTime(latest), note(group));
	}

	/** "Ansökan: " and the declared types, so the handläggare can tell the row from one SSBTEK filled. */
	private static String note(final List<Resolved> group) {
		return group.stream()
			.map(resolved -> ofNullable(resolved.income().label()).orElse(resolved.income().incomeType()))
			.distinct()
			.collect(joining(", ", NOTE_PREFIX, ""));
	}

	private static String recipient(final ApplicationIncome income) {
		if (RECIPIENT_CO_APPLICANT.equals(income.recipient())) {
			return RECIPIENT_CO_APPLICANT;
		}
		return RECIPIENT_APPLICANT;
	}

	private static boolean hasAmount(final BigDecimal amount) {
		return (amount != null) && (amount.signum() != 0);
	}

	private static Map<Integer, String> indexIncomeTypeNamesById(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(PersonBasedCalculationCalculationIncomeTypeDTO::getId, PersonBasedCalculationCalculationIncomeTypeDTO::getName, (first, _) -> first, LinkedHashMap::new));
	}

	private record Resolved(Integer typeId, ApplicationIncome income) {}
}
