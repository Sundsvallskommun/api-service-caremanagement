package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

/**
 * Maps incomes already classified by the operaton rules to FC calculation income rows. The raw list decision is
 * done — each income carries its target calculation category; this mapper only resolves that category to the numeric
 * FC type id offered by the calculation proposal and merges incomes of the same type into one row (applicant and
 * co-applicant amounts summed into their own columns). Off-list / "ej ta med" incomes (no {@code TA_MED} action or no
 * category) are skipped — their warnings already come from operaton. This is the plumbing half of the former
 * {@code SsbtekToFcIncomeMapper}; the raw list half moved to the engine.
 */
public final class ClassifiedIncomeToFcMapper {

	private static final String TRANSFER_ACTION_PREFIX = "TA_MED";

	private ClassifiedIncomeToFcMapper() {}

	/**
	 * Map the classified incomes to FC calculation rows for the given calculation proposal.
	 *
	 * @param  classified the incomes classified by the operaton rules (maybe {@code null})
	 * @param  proposal   the FC calculation proposal whose {@code calculationIncomeTypes} supply the numeric type ids
	 * @return            the FC income rows (incomes resolving to the same type id are merged)
	 */
	public static List<PersonBasedCalculationIncomePostDTO> toCalculationIncomes(final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = MapperUtil.indexIncomeTypeIds(proposal);

		return ofNullable(classified).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(ClassifiedIncomeToFcMapper::isTransferable)
			.map(income -> resolve(income, typeIdByName))
			.filter(Objects::nonNull)
			// Drop role-less incomes — they can't be attributed to the applicant or co-applicant amount, so (consistent
			// with toIncomeLines) they must not leak into the note either.
			.filter(resolved -> resolved.income().role() != null)
			.collect(groupingBy(Resolved::typeId, LinkedHashMap::new, toList()))
			.entrySet().stream()
			.map(entry -> toDto(entry.getKey(), entry.getValue()))
			.toList();
	}

	/**
	 * Map the classified incomes to draft income lines — one line per (FC income type, recipient), the granularity the
	 * calculation draft stores so a caseworker can override or soft-delete a single person's income of a type. The same
	 * transferability + type-id resolution as {@link #toCalculationIncomes} is used; the difference is the rows are not
	 * folded across recipients.
	 *
	 * @param  classified the incomes classified by the operaton rules (maybe {@code null})
	 * @param  proposal   the FC calculation proposal whose {@code calculationIncomeTypes} supply the numeric type ids +
	 *                    names
	 * @return            one income line per (type id, recipient), amounts summed within the pair
	 */
	public static List<FcIncomeLine> toIncomeLines(final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = MapperUtil.indexIncomeTypeIds(proposal);
		final var nameById = indexIncomeTypeNamesById(proposal);

		return ofNullable(classified).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(ClassifiedIncomeToFcMapper::isTransferable)
			.map(income -> resolve(income, typeIdByName))
			.filter(Objects::nonNull)
			// A classified income with no role can't be folded per-recipient — drop it (consistent with the
			// calculation-incomes path, which sums per role) rather than NPE on role().name() in the grouping key.
			.filter(resolved -> resolved.income().role() != null)
			.collect(groupingBy(resolved -> resolved.typeId() + "|" + resolved.income().role().name(), LinkedHashMap::new, toList()))
			.values().stream()
			.map(group -> toLine(group, nameById))
			.toList();
	}

	private static FcIncomeLine toLine(final List<Resolved> group, final Map<Integer, String> nameById) {
		final var typeId = group.getFirst().typeId();
		final var role = group.getFirst().income().role();
		return new FcIncomeLine(typeId, nameById.get(typeId), role.name(),
			sumByRole(group, role), MapperUtil.toOffsetDateTime(latestDateByRole(group, role)), noteFor(group));
	}

	private static Map<Integer, String> indexIncomeTypeNamesById(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(PersonBasedCalculationCalculationIncomeTypeDTO::getId, PersonBasedCalculationCalculationIncomeTypeDTO::getName, (first, second) -> first, LinkedHashMap::new));
	}

	/**
	 * The previous-month FC income-type names not covered by this month's classified incomes — the basis for the financial
	 * assistance "all
	 * last month's calculation values present" completeness check. Matching is on the normalised type name, the same key
	 * {@link #toCalculationIncomes} resolves on, so the two months compare like-for-like. An empty result means every
	 * previous income type has a transferable income this month (i.e. the information is complete).
	 *
	 * @param  previousTypeNames the income-type names on the previous calculation (FC {@code getType()})
	 * @param  classified        this month's classified incomes
	 * @param  proposal          this month's FC calculation proposal (supplies the valid type names)
	 * @return                   the previous type names with no transferable income this month
	 */
	public static List<String> missingPreviousIncomeTypes(final List<String> previousTypeNames,
		final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {

		final var covered = coveredTypeNames(classified, proposal);
		return ofNullable(previousTypeNames).orElseGet(List::of).stream()
			.filter(name -> (name != null) && !name.isBlank())
			.distinct()
			.filter(name -> !covered.contains(MapperUtil.normalize(name)))
			.toList();
	}

	/** The normalised FC income-type names this month's transferable classified incomes resolve to. */
	private static Set<String> coveredTypeNames(final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = MapperUtil.indexIncomeTypeIds(proposal);
		return ofNullable(classified).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(ClassifiedIncomeToFcMapper::isTransferable)
			.map(income -> MapperUtil.normalize(income.calculation()))
			.filter(typeIdByName::containsKey)
			.collect(toSet());
	}

	private static boolean isTransferable(final ClassifiedIncome classified) {
		return (classified.action() != null) && classified.action().startsWith(TRANSFER_ACTION_PREFIX)
			&& (classified.calculation() != null) && !classified.calculation().isBlank() && !"-".equals(classified.calculation());
	}

	private static Resolved resolve(final ClassifiedIncome classified, final Map<String, Integer> typeIdByName) {
		final var typeId = typeIdByName.get(MapperUtil.normalize(classified.calculation()));
		if (typeId == null) {
			return null;
		}
		return new Resolved(typeId, classified.income());
	}

	private static PersonBasedCalculationIncomePostDTO toDto(final Integer typeId, final List<Resolved> group) {
		return new PersonBasedCalculationIncomePostDTO()
			.id(typeId)
			.applicantAmount(toDouble(sumByRole(group, APPLICANT)))
			.applicantAmountDate(MapperUtil.toOffsetDateTime(latestDateByRole(group, APPLICANT)))
			.coApplicantAmount(toDouble(sumByRole(group, CO_APPLICANT)))
			.coApplicantAmountDate(MapperUtil.toOffsetDateTime(latestDateByRole(group, CO_APPLICANT)))
			.note(noteFor(group));
	}

	private static BigDecimal sumByRole(final List<Resolved> group, final ApplicantRole role) {
		return group.stream()
			.map(Resolved::income)
			.filter(income -> income.role() == role)
			.map(SsbtekIncome::netAmount)
			.filter(Objects::nonNull)
			.reduce(BigDecimal::add)
			.orElse(null);
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
		return Stream.of(income.benefit(), income.subBenefit(), income.amountType())
			.filter(Objects::nonNull)
			.filter(value -> !value.isBlank())
			.collect(joining(" / "));
	}

	private static Double toDouble(final BigDecimal value) {
		return ofNullable(value).map(BigDecimal::doubleValue).orElse(null);
	}

	/** An income that resolved to a concrete FC income-type id, pending aggregation. */
	private record Resolved(Integer typeId, SsbtekIncome income) {
	}
}
