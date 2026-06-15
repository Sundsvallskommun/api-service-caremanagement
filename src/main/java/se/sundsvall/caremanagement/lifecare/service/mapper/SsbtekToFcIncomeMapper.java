package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncomeMappingResult;
import se.sundsvall.caremanagement.lifecare.service.model.UnhandledIncome;
import se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason.FC_TYPE_NOT_IN_PROPOSAL;
import static se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason.NOT_ON_WHITELIST;

/**
 * Maps normalised SSBTEK incomes to FC normberäkning income rows. For each income it looks up the FC normberäkning
 * income-type name in {@link SsbtekIncomeRegistry}, resolves that name to the numeric type id offered by the FC
 * calculation proposal, then merges incomes that resolve to the same type id into one
 * {@link PersonBasedCalculationIncomePostDTO} (applicant and co-applicant amounts summed into their own columns).
 * Incomes that are not on the whitelist, or whose FC type the proposal does not offer, are returned as
 * {@link UnhandledIncome} warnings rather than silently dropped — except deliberate exclusions
 * (regelverk "Ej ta med"), which are skipped without a warning. Period selection (which incomes to transfer) is the
 * caller's responsibility; this mapper sums whatever it is given. See ssbtek-regelverk.txt.
 */
public final class SsbtekToFcIncomeMapper {

	private SsbtekToFcIncomeMapper() {}

	/**
	 * Map a set of SSBTEK incomes to FC normberäkning rows for the given calculation proposal.
	 *
	 * @param  incomes  the normalised SSBTEK incomes to transfer (may be {@code null})
	 * @param  proposal the FC calculation proposal whose {@code calculationIncomeTypes} supply the numeric type ids
	 * @return          the FC income rows plus the incomes that could not be transferred
	 */
	public static SsbtekIncomeMappingResult toCalculationIncomes(final List<SsbtekIncome> incomes, final PersonBasedCalculationProposalDTO proposal) {
		final var typeIdByName = indexIncomeTypeIds(proposal);
		final var unhandled = new ArrayList<UnhandledIncome>();
		final var resolved = new ArrayList<Resolved>();

		ofNullable(incomes).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.forEach(income -> resolve(income, typeIdByName, resolved, unhandled));

		final var rows = resolved.stream()
			.collect(groupingBy(Resolved::typeId, LinkedHashMap::new, toList()))
			.entrySet().stream()
			.map(entry -> toDto(entry.getKey(), entry.getValue()))
			.toList();

		return new SsbtekIncomeMappingResult(rows, List.copyOf(unhandled));
	}

	private static void resolve(final SsbtekIncome income, final Map<String, Integer> typeIdByName, final List<Resolved> resolved, final List<UnhandledIncome> unhandled) {
		final var fcTypeName = SsbtekIncomeRegistry.fcNormberakningType(income.forman());
		if (fcTypeName.isEmpty()) {
			if (!SsbtekIncomeRegistry.isExcluded(income.forman())) {
				unhandled.add(toUnhandled(income, NOT_ON_WHITELIST));
			}
			return;
		}

		final var typeId = typeIdByName.get(SsbtekIncomeRegistry.normalize(fcTypeName.get()));
		if (typeId == null) {
			unhandled.add(toUnhandled(income, FC_TYPE_NOT_IN_PROPOSAL));
			return;
		}

		resolved.add(new Resolved(typeId, income));
	}

	private static Map<String, Integer> indexIncomeTypeIds(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> SsbtekIncomeRegistry.normalize(type.getName()), PersonBasedCalculationCalculationIncomeTypeDTO::getId, (first, second) -> first, LinkedHashMap::new));
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

		if (amounts.isEmpty()) {
			return null;
		}
		return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
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
		// Resolved incomes always have a non-blank förmån, so describe() is never blank here.
		return "SSBTEK: " + group.stream()
			.map(Resolved::income)
			.map(SsbtekToFcIncomeMapper::describe)
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

	private static UnhandledIncome toUnhandled(final SsbtekIncome income, final UnhandledReason reason) {
		return new UnhandledIncome(income.forman(), income.delforman(), income.beloppstyp(), reason);
	}

	/** An income that resolved to a concrete FC income-type id, pending aggregation. */
	private record Resolved(Integer typeId, SsbtekIncome income) {
	}
}
