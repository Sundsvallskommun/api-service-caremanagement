package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

/**
 * Maps incomes already classified by the operaton regelverk to FC normberäkning income rows. The rålista decision is
 * done — each income carries its target normberäkning category; this mapper only resolves that category to the numeric
 * FC type id offered by the calculation proposal and merges incomes of the same type into one row (applicant and
 * co-applicant amounts summed into their own columns). Off-list / "ej ta med" incomes (no {@code TA_MED} action or no
 * category) are skipped — their warnings already come from operaton. This is the plumbing half of the former
 * {@code SsbtekToFcIncomeMapper}; the rålista half moved to the engine.
 */
public final class ClassifiedIncomeToFcMapper {

	private static final String TRANSFER_ACTION_PREFIX = "TA_MED";

	private ClassifiedIncomeToFcMapper() {}

	/**
	 * Map the classified incomes to FC normberäkning rows for the given calculation proposal.
	 *
	 * @param  classified the incomes classified by the operaton regelverk (maybe {@code null})
	 * @param  proposal   the FC calculation proposal whose {@code calculationIncomeTypes} supply the numeric type ids
	 * @return            the FC income rows (incomes resolving to the same type id are merged)
	 */
	public static List<PersonBasedCalculationIncomePostDTO> toCalculationIncomes(final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = indexIncomeTypeIds(proposal);

		return ofNullable(classified).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(ClassifiedIncomeToFcMapper::isTransferable)
			.map(income -> resolve(income, typeIdByName))
			.filter(Objects::nonNull)
			.collect(groupingBy(Resolved::typeId, LinkedHashMap::new, toList()))
			.entrySet().stream()
			.map(entry -> toDto(entry.getKey(), entry.getValue()))
			.toList();
	}

	private static boolean isTransferable(final ClassifiedIncome classified) {
		return (classified.atgard() != null) && classified.atgard().startsWith(TRANSFER_ACTION_PREFIX)
			&& (classified.normberakning() != null) && !classified.normberakning().isBlank() && !"-".equals(classified.normberakning());
	}

	private static Resolved resolve(final ClassifiedIncome classified, final Map<String, Integer> typeIdByName) {
		final var typeId = typeIdByName.get(normalize(classified.normberakning()));
		return (typeId == null) ? null : new Resolved(typeId, classified.income());
	}

	private static Map<String, Integer> indexIncomeTypeIds(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> normalize(type.getName()), PersonBasedCalculationCalculationIncomeTypeDTO::getId, (first, second) -> first, LinkedHashMap::new));
	}

	private static PersonBasedCalculationIncomePostDTO toDto(final Integer typeId, final List<Resolved> group) {
		return new PersonBasedCalculationIncomePostDTO()
			.id(typeId)
			.applicantAmount(toDouble(sumByRole(group, APPLICANT)))
			.applicantAmountDate(toOffsetDateTime(latestDateByRole(group, APPLICANT)))
			.coApplicantAmount(toDouble(sumByRole(group, CO_APPLICANT)))
			.coApplicantAmountDate(toOffsetDateTime(latestDateByRole(group, CO_APPLICANT)))
			.note(noteFor(group));
	}

	private static BigDecimal sumByRole(final List<Resolved> group, final ApplicantRole role) {
		final var amounts = group.stream()
			.map(Resolved::income)
			.filter(income -> income.role() == role)
			.map(SsbtekIncome::netAmount)
			.filter(Objects::nonNull)
			.toList();
		return amounts.isEmpty() ? null : amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static LocalDate latestDateByRole(final List<Resolved> group, final ApplicantRole role) {
		return group.stream()
			.map(Resolved::income)
			.filter(income -> income.role() == role)
			.map(SsbtekIncome::period)
			.filter(Objects::nonNull)
			.max(Comparator.naturalOrder())
			.orElse(null);
	}

	private static String noteFor(final List<Resolved> group) {
		return "SSBTEK: " + group.stream()
			.map(Resolved::income)
			.map(ClassifiedIncomeToFcMapper::describe)
			.distinct()
			.collect(joining("; "));
	}

	private static String describe(final SsbtekIncome income) {
		return Stream.of(income.forman(), income.delforman(), income.beloppstyp())
			.filter(Objects::nonNull)
			.filter(value -> !value.isBlank())
			.collect(joining(" / "));
	}

	private static Double toDouble(final BigDecimal value) {
		return ofNullable(value).map(BigDecimal::doubleValue).orElse(null);
	}

	private static OffsetDateTime toOffsetDateTime(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().atOffset(ZoneOffset.UTC)).orElse(null);
	}

	private static String normalize(final String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}

	/** An income that resolved to a concrete FC income-type id, pending aggregation. */
	private record Resolved(Integer typeId, SsbtekIncome income) {
	}
}
