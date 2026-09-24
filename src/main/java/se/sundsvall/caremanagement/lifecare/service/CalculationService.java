package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpensePostDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.caremanagement.lifecare.service.mapper.ApplicationIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.CalculationAssembler;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationSections;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper.BUCKET_SPECIAL_EXPENSE;
import static se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil.toWireAmount;

/**
 * Builds and posts the calculation to Lifecare FamilyCare from incomes already classified by the operaton rules.
 * caremanagement no longer fetches SSBTEK or evaluates the raw list — the rules live in the process. This service
 * resolves each classified income's category to a FamilyCare income-type id (via {@link
 * ClassifiedIncomeToFamilyCareMapper}), assembles the calculation against the applicant's proposal (via {@link
 * CalculationAssembler}), and posts it. It also reports whether this month's calculation covers every income type the
 * previous month's did — the financial assistance process polls SSBTEK daily until it does.
 */
@Service
public class CalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(CalculationService.class);
	private static final String UNKNOWN_INCOME_TYPE = "Income row '%s' has no Lifecare income type id and its name matches none of Lifecare's income types";

	private final LifecareFamilyCare lifecareFamilyCareIntegration;
	private final LifecareCaseService lifecareCaseService;
	private final ObjectMapper objectMapper;

	public CalculationService(final LifecareFamilyCare lifecareFamilyCareIntegration, final LifecareCaseService lifecareCaseService,
		final ObjectMapper objectMapper) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
		this.lifecareCaseService = lifecareCaseService;
		this.objectMapper = objectMapper;
	}

	/**
	 * The classified incomes as the engine sent them — both periods, nothing filtered. The transfer path narrows this
	 * ({@link #incomeLines} drops what the previous month already took); the period checks need the unfiltered list,
	 * because a comparison-period föräldrapenning is what the gap is measured against whether or not it was
	 * transferred.
	 *
	 * @param  classifiedIncomesJson the {@code classifiedIncomes} payload
	 * @return                       the parsed incomes
	 */
	public List<ClassifiedIncome> classifiedIncomes(final String classifiedIncomesJson) {
		return parse(classifiedIncomesJson);
	}

	/**
	 * The process-derived income lines for the draft — one per (FamilyCare income type, recipient) — from the
	 * operaton-classified incomes resolved against the applicant's calculation proposal. Comparison-period incomes the
	 * previous month already transferred are dropped first. Writes nothing to Lifecare.
	 */
	public List<FamilyCareIncomeLine> incomeLines(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		final var transferable = ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(
			parse(classifiedIncomesJson), previousIncomeTypes(municipalityId, applicantPersonId, applicationMonth));
		return ClassifiedIncomeToFamilyCareMapper.toIncomeLines(transferable, proposal);
	}

	/**
	 * The comparison-period incomes this month's transfer picks up <em>because</em> the previous month's calculation
	 * did not contain them — the “nödlösning” case {@link ClassifiedIncomeToFamilyCareMapper#withoutAlreadyTransferred}
	 * describes, seen from the other side.
	 * <p>
	 * Verksamhetens regelverk (revision 2026-09-22) added a warning for exactly this set: <em>”Finns inkomster i
	 * jämförelseperioden som inte är överförda? Ja = för över till normberäkning och generera varning”</em>. The
	 * transfer half has been in place all along; this exposes which incomes it moved so the warning can name them.
	 * <p>
	 * Derived by running the same filter as {@link #incomeLines}, not by re-deriving the rule — the two must not be
	 * able to disagree about what was transferred. The cost is one more previous-calculation read per prepare, in
	 * company with the ones {@code refreshDraft} already makes for amounts, household and expenses.
	 *
	 * @param  classifiedIncomesJson the {@code classifiedIncomes} payload
	 * @return                       the comparison-period incomes being transferred now, in engine order
	 */
	public List<ClassifiedIncome> lateTransferredComparisonIncomes(final String municipalityId, final String applicantPersonId,
		final YearMonth applicationMonth, final String classifiedIncomesJson) {
		return ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(
			parse(classifiedIncomesJson), previousIncomeTypes(municipalityId, applicantPersonId, applicationMonth)).stream()
			.filter(ClassifiedIncome::isFromComparisonPeriod)
			.toList();
	}

	/**
	 * The income types on the previous month's calculation, best-effort. A failed read yields none, which transfers the
	 * comparison-period incomes as before: an income counted twice shows up as a duplicate warning on the case, an
	 * income silently withheld shows up as nothing at all.
	 */
	private List<String> previousIncomeTypes(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth) {
		try {
			return lifecareCaseService.previousCalculationIncomeTypes(municipalityId, applicantPersonId, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous month's calculation — transferring the comparison period unfiltered", e);
			return List.of();
		}
	}

	/**
	 * The process-derived income lines for a calculation built straight from the incomes the citizen declared in the
	 * application — the new application sibling of {@link #incomeLines}, no SSBTEK. Each application income code is
	 * translated to its FamilyCare income type and resolved against the applicant's calculation proposal (via {@link
	 * ApplicationIncomeToFamilyCareMapper}); incomes whose type does not resolve are skipped. Same {@link
	 * FamilyCareIncomeLine} shape as the SSBTEK path, so the downstream fold + commit pipeline is shared. Writes nothing
	 * to Lifecare.
	 */
	public List<FamilyCareIncomeLine> applicationIncomeLines(final String municipalityId, final String applicantPersonId, final List<ApplicationIncome> incomes) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		return ApplicationIncomeToFamilyCareMapper.toIncomeLines(incomes, proposal);
	}

	/**
	 * Whether this month's classified incomes cover every income type the previous calculation had. Best-effort: a failure
	 * reading the previous month is treated as complete so the financial assistance process is not wedged.
	 */
	public Completeness completeness(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		final var missing = missingPreviousIncomeTypes(municipalityId, applicantPersonId, applicationMonth, parse(classifiedIncomesJson), proposal);
		return new Completeness(missing.isEmpty(), missing);
	}

	/**
	 * The norm id for the application month: the first of the requested norm <em>names</em> found among the norms
	 * whose window covers it, or {@code null} when the proposal offers none. The caller resolves its own norm-type
	 * vocabulary to names before calling.
	 */
	public Integer selectNormId(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth,
		final List<String> normNames) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		return CalculationAssembler.selectNormId(proposal, applicationMonth, normNames).orElse(null);
	}

	/**
	 * The norm chosen for a month, and whether it is the preferred one.
	 *
	 * @param normId           the chosen norm id, {@code null} when the proposal offers none
	 * @param preferredMatched whether a preferred name matched a norm covering the month
	 */
	public record NormChoice(Integer normId, boolean preferredMatched) {}

	/**
	 * The norm id for the application month, preferring a norm named by {@code preferredNames} (matched strictly, see
	 * {@link CalculationAssembler#matchingNormId}) and otherwise choosing from {@code fallbackNames} as
	 * {@link #selectNormId(String, String, YearMonth, List)} does. One proposal read either way.
	 */
	public NormChoice selectNormId(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth,
		final List<String> preferredNames, final List<String> fallbackNames) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		return CalculationAssembler.matchingNormId(proposal, applicationMonth, preferredNames)
			.map(normId -> new NormChoice(normId, true))
			.orElseGet(() -> new NormChoice(CalculationAssembler.selectNormId(proposal, applicationMonth, fallbackNames).orElse(null), false));
	}

	/**
	 * Create the calculation in Lifecare FamilyCare from the draft's effective rows — called on a decision. Folds the
	 * effective incomes, expenses (resolving each cost type to a FamilyCare expense-type id, skipping the unresolvable)
	 * and household persons into the FamilyCare body, overriding the proposal norm with the one chosen on the draft, and
	 * posts it.
	 *
	 * @return the created Lifecare calculation id
	 */
	public Integer commitEffective(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth, final CalculationHeader header,
		final List<EffectiveIncome> incomes, final List<EffectiveExpense> expenses, final List<EffectivePerson> persons) {

		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		final var incomeTypeIds = MapperUtil.indexIncomeTypeIds(proposal);
		final var incomeDtos = ofNullable(incomes).orElseGet(List::of).stream().map(income -> toIncomeDto(income, incomeTypeIds)).toList();

		final var allExpenses = ofNullable(expenses).orElseGet(List::of);
		final var expenseDtos = allExpenses.stream()
			.filter(expense -> !BUCKET_SPECIAL_EXPENSE.equals(expense.bucket()))
			.map(expense -> toExpenseDto(expense, proposal)).filter(Objects::nonNull).toList();
		final var specialExpenseDtos = allExpenses.stream()
			.filter(expense -> BUCKET_SPECIAL_EXPENSE.equals(expense.bucket()))
			.map(expense -> toSpecialExpenseDto(expense, proposal)).filter(Objects::nonNull).toList();

		final var periodDays = familyCarePeriodDays(header, applicationMonth);
		final var personDtos = ofNullable(persons).orElseGet(List::of).stream().map(person -> toPersonDto(person, periodDays)).toList();

		final var sections = new CalculationSections(incomeDtos, expenseDtos, specialExpenseDtos, personDtos, header);
		// No norm types here: the draft header carries the norm already chosen by selectNormId and overrides whatever
		// the assembler would pick, so this argument only feeds a fallback that the header makes unreachable.
		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, sections, applicationMonth, List.of());
		return lifecareFamilyCareIntegration.createCalculation(municipalityId, body);
	}

	private static PersonBasedCalculationIncomePostDTO toIncomeDto(final EffectiveIncome income, final Map<String, Integer> incomeTypeIds) {
		return new PersonBasedCalculationIncomePostDTO()
			.id(incomeTypeId(income, incomeTypeIds))
			.applicantAmount(toWireAmount(income.applicantAmount()))
			.applicantAmountDate(income.applicantAmountDate())
			.coApplicantAmount(toWireAmount(income.coApplicantAmount()))
			.coApplicantAmountDate(income.coApplicantAmountDate())
			.note(income.note());
	}

	/**
	 * The row's FamilyCare income type id: its own when set, otherwise the proposal's id for its type name. An income
	 * that resolves to neither is refused rather than dropped the way an unknown expense is — leaving out an income
	 * changes the amount the person is granted, and posting it without an id is refused by Lifecare anyway.
	 */
	private static Integer incomeTypeId(final EffectiveIncome income, final Map<String, Integer> incomeTypeIds) {
		return ofNullable(income.typeId())
			.or(() -> ofNullable(incomeTypeIds.get(MapperUtil.normalize(income.typeName()))))
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, UNKNOWN_INCOME_TYPE.formatted(income.typeName())));
	}

	private static PersonBasedCalculationExpensePostDTO toExpenseDto(final EffectiveExpense expense, final PersonBasedCalculationProposalDTO proposal) {
		return ExpenseTypeMapper.resolveExpenseTypeId(expense.costType(), proposal, expense.bucket())
			.map(id -> new PersonBasedCalculationExpensePostDTO().id(id).amount(toWireAmount(expense.appliedAmount())).approvedAmount(toWireAmount(expense.approvedAmount())).note(expense.note()))
			.orElse(null);
	}

	private static PersonBasedCalculationSpecialExpensePostDTO toSpecialExpenseDto(final EffectiveExpense expense, final PersonBasedCalculationProposalDTO proposal) {
		return ExpenseTypeMapper.resolveExpenseTypeId(expense.costType(), proposal, expense.bucket())
			.map(id -> new PersonBasedCalculationSpecialExpensePostDTO().id(id).amount(toWireAmount(expense.appliedAmount())).approvedAmount(toWireAmount(expense.approvedAmount())).note(expense.note()))
			.orElse(null);
	}

	/**
	 * A household row for the FamilyCare body. The {@code personId} is deliberately careM's {@code partyId} and not a
	 * personal identity number: it is the identity careM holds, it is what the integrator route wants unchanged, and
	 * the direct route resolves it to a personal identity number at its own edge in
	 * {@code LifecareFamilyCareIntegration.createCalculation}. Resolving it here instead would make the integrator
	 * route round-trip party → personnummer → party for no gain, and fail where the reverse lookup does.
	 */
	private static PersonBasedCalculationPersonPostDTO toPersonDto(final EffectivePerson person, final long periodDays) {
		return new PersonBasedCalculationPersonPostDTO()
			.personId(person.partyId())
			.numberOfDays(cappedDays(person.numberOfDays(), periodDays))
			.deviationFromDate(toOffsetDateTime(person.deviationFromDate()))
			.deviationToDate(toOffsetDateTime(person.deviationToDate()));
	}

	/**
	 * The largest {@code NumberOfDays} FamilyCare accepts over the calculation period: the difference between its first
	 * and last day, <em>not</em> the inclusive day count. September 2026 (2026-09-01–2026-09-30) is 29, and 30 is
	 * refused with {@code Invalid NumberOfDays for calculationperson}.
	 *
	 * <p>
	 * Established against the live API on 2026-09-22, because the FamilyCare specification says nothing about the
	 * field and the read model does not carry it at all — an existing calculation cannot be inspected to learn the
	 * rule. Whether FamilyCare means "days between" or merely enforces an upper bound is not settled by that one
	 * observation; both readings accept this value, so it is used as a cap rather than as a computed answer.
	 */
	private static long familyCarePeriodDays(final CalculationHeader header, final YearMonth applicationMonth) {
		final var from = ofNullable(header).map(CalculationHeader::calculationFromDate).orElseGet(() -> applicationMonth.atDay(1));
		final var to = ofNullable(header).map(CalculationHeader::calculationToDate).orElseGet(applicationMonth::atEndOfMonth);
		return DAYS.between(ofNullable(from).orElseGet(() -> applicationMonth.atDay(1)), ofNullable(to).orElseGet(applicationMonth::atEndOfMonth));
	}

	/**
	 * A household member's days, never above what the period allows. careM counts a full month as 30 whatever its
	 * length, which is right for the caseworker reading the draft and wrong on the wire for every month that is not
	 * 31 days long — so the conversion happens here, at the FamilyCare edge, and the draft keeps the number a human
	 * recognises.
	 */
	private static Integer cappedDays(final Integer days, final long periodDays) {
		if (days == null) {
			return null;
		}
		return (int) Math.min(days.longValue(), periodDays);
	}

	private static OffsetDateTime toOffsetDateTime(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().atOffset(ZoneOffset.UTC)).orElse(null);
	}

	private List<String> missingPreviousIncomeTypes(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth,
		final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		try {
			final var previousTypes = lifecareCaseService.previousCalculationIncomeTypes(municipalityId, applicantPersonId, applicationMonth);
			return ClassifiedIncomeToFamilyCareMapper.missingPreviousIncomeTypes(previousTypes, classified, proposal);
		} catch (final RuntimeException e) {
			LOG.warn("Could not determine calculation completeness against the previous month — treating as complete", e);
			return List.of();
		}
	}

	private List<ClassifiedIncome> parse(final String classifiedIncomesJson) {
		try {
			return List.of(objectMapper.readValue(classifiedIncomesJson, ClassifiedIncome[].class));
		} catch (final JacksonException e) {
			throw Problem.valueOf(BAD_REQUEST, "Invalid classifiedIncomes JSON");
		}
	}
}
