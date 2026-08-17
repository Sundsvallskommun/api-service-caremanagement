package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpensePostDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ApplicationIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.CalculationAssembler;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationDraftBuild;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationResult;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationSections;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.DraftRow;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper.BUCKET_SPECIAL_EXPENSE;

/**
 * Builds and posts the calculation to Lifecare FamilyCare from incomes already classified by the operaton rules.
 * caremanagement no longer fetches SSBTEK or evaluates the raw list — the rules live in the process. This service
 * resolves each classified income's category to an FamilyCare income-type id (via
 * {@link ClassifiedIncomeToFamilyCareMapper}),
 * assembles
 * the calculation against the applicant's proposal (via {@link CalculationAssembler}), and posts it. It also reports
 * whether this month's calculation covers every income type the previous month's did — the financial assistance process
 * polls SSBTEK
 * daily until it does.
 *
 * <p>
 * <strong>WIP</strong>: {@code buildAndPostFromClassified}, {@code buildDraft} and {@code postDraftRows} are not yet
 * wired into a production caller — they are currently only exercised by tests. TODO: complete the draft / post-back
 * flow (or drop the unused entry points) in a following sprint.
 */
@Service
public class CalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(CalculationService.class);

	private final LifecareFamilyCareIntegration lifecareFamilyCareIntegration;
	private final LifecareCaseService lifecareCaseService;
	private final ObjectMapper objectMapper;

	public CalculationService(final LifecareFamilyCareIntegration lifecareFamilyCareIntegration, final LifecareCaseService lifecareCaseService,
		final ObjectMapper objectMapper) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
		this.lifecareCaseService = lifecareCaseService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Build and post the calculation from incomes already classified by the operaton rules, and report whether the
	 * information is complete — i.e. whether every income type on the previous month's calculation is present this
	 * month. The completeness lookup is best-effort: a failure reading the previous calculation never fails the just-
	 * created calculation; it is reported as complete (with no missing types) so the process is not wedged.
	 *
	 * @param  applicantPersonId     the applicant's personal identity number (the FamilyCare proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the created calculation id plus the completeness verdict + missing income types
	 */
	public CalculationResult buildAndPostFromClassified(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFamilyCareMapper.toCalculationIncomes(classified, proposal);
		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		final var calculationId = lifecareFamilyCareIntegration.createCalculation(body);

		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new CalculationResult(calculationId, missing.isEmpty(), missing);
	}

	/**
	 * Build the draft calculation from the classified incomes — the FamilyCare income rows plus the completeness verdict —
	 * <strong>without</strong> creating anything in Lifecare. The financial assistance process stores the rows as an
	 * editable draft and
	 * polls SSBTEK daily until the information is complete; the Lifecare calculation is created later, on a decision, from
	 * the (possibly edited) draft (see {@link #postDraftRows}).
	 *
	 * @param  applicantPersonId     the applicant's personal identity number (the FamilyCare proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the FamilyCare income rows + completeness verdict; nothing is written to Lifecare
	 */
	public CalculationDraftBuild buildDraft(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFamilyCareMapper.toCalculationIncomes(classified, proposal);
		final var typeNamesById = incomeTypeNamesById(proposal);
		final var rows = incomes.stream().map(income -> toDraftRow(income, typeNamesById)).toList();
		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new CalculationDraftBuild(rows, missing.isEmpty(), missing);
	}

	/**
	 * Create the calculation in Lifecare FamilyCare from the (possibly caseworker-edited) draft income rows — called on a
	 * decision. Assembles against the applicant's proposal and posts; returns the created calculation id.
	 */
	public Integer postDraftRows(final String applicantPersonId, final YearMonth applicationMonth, final List<DraftRow> rows) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ofNullable(rows).orElseGet(List::of).stream().map(CalculationService::toPostDto).toList();
		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		return lifecareFamilyCareIntegration.createCalculation(body);
	}

	/**
	 * The process-derived income lines for the draft — one per (FamilyCare income type, recipient) — from the
	 * operaton-classified
	 * incomes resolved against the applicant's calculation proposal. Writes nothing to Lifecare.
	 */
	public List<FamilyCareIncomeLine> incomeLines(final String applicantPersonId, final String classifiedIncomesJson) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		return ClassifiedIncomeToFamilyCareMapper.toIncomeLines(parse(classifiedIncomesJson), proposal);
	}

	/**
	 * The process-derived income lines for a calculation built straight from the incomes the citizen declared in the
	 * application — the new application sibling of {@link #incomeLines}, no SSBTEK. Each application income code is
	 * translated to
	 * its FamilyCare income type and resolved against the applicant's calculation proposal (via
	 * {@link ApplicationIncomeToFamilyCareMapper});
	 * incomes whose type does not resolve are skipped. Same {@link FamilyCareIncomeLine} shape as the SSBTEK path, so the
	 * downstream fold + commit pipeline is shared. Writes nothing to Lifecare.
	 */
	public List<FamilyCareIncomeLine> applicationIncomeLines(final String applicantPersonId, final List<ApplicationIncome> incomes) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		return ApplicationIncomeToFamilyCareMapper.toIncomeLines(incomes, proposal);
	}

	/**
	 * Whether this month's classified incomes cover every income type the previous calculation had. Best-effort: a
	 * failure reading the previous month is treated as complete so the financial assistance process is not wedged.
	 */
	public Completeness completeness(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, parse(classifiedIncomesJson), proposal);
		return new Completeness(missing.isEmpty(), missing);
	}

	/**
	 * The norm id the FamilyCare proposal offers for the application month (covering window, else first), or {@code null}.
	 */
	public Integer selectNormId(final String applicantPersonId, final YearMonth applicationMonth) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		return CalculationAssembler.selectNormId(proposal, applicationMonth).orElse(null);
	}

	/**
	 * Create the calculation in Lifecare FamilyCare from the draft's effective rows — called on a decision. Folds the
	 * effective
	 * incomes, expenses (resolving each cost type to an FamilyCare expense-type id, skipping the unresolvable) and
	 * household
	 * persons into the FamilyCare body, overriding the proposal norm with the one chosen on the draft, and posts it.
	 *
	 * @return the created Lifecare calculation id
	 */
	public Integer commitEffective(final String applicantPersonId, final YearMonth applicationMonth, final CalculationHeader header,
		final List<EffectiveIncome> incomes, final List<EffectiveExpense> expenses, final List<EffectivePerson> persons) {

		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		final var incomeDtos = ofNullable(incomes).orElseGet(List::of).stream().map(CalculationService::toIncomeDto).toList();

		final var allExpenses = ofNullable(expenses).orElseGet(List::of);
		final var expenseDtos = allExpenses.stream()
			.filter(expense -> !BUCKET_SPECIAL_EXPENSE.equals(expense.bucket()))
			.map(expense -> toExpenseDto(expense, proposal)).filter(Objects::nonNull).toList();
		final var specialExpenseDtos = allExpenses.stream()
			.filter(expense -> BUCKET_SPECIAL_EXPENSE.equals(expense.bucket()))
			.map(expense -> toSpecialExpenseDto(expense, proposal)).filter(Objects::nonNull).toList();

		final var personDtos = ofNullable(persons).orElseGet(List::of).stream().map(CalculationService::toPersonDto).toList();

		final var sections = new CalculationSections(incomeDtos, expenseDtos, specialExpenseDtos, personDtos, header);
		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, sections, applicationMonth);
		return lifecareFamilyCareIntegration.createCalculation(body);
	}

	private static PersonBasedCalculationIncomePostDTO toIncomeDto(final EffectiveIncome income) {
		return new PersonBasedCalculationIncomePostDTO()
			.id(income.typeId())
			.applicantAmount(income.applicantAmount())
			.applicantAmountDate(income.applicantAmountDate())
			.coApplicantAmount(income.coApplicantAmount())
			.coApplicantAmountDate(income.coApplicantAmountDate())
			.note(income.note());
	}

	private static PersonBasedCalculationExpensePostDTO toExpenseDto(final EffectiveExpense expense, final PersonBasedCalculationProposalDTO proposal) {
		return ExpenseTypeMapper.resolveExpenseTypeId(expense.costType(), proposal, expense.bucket())
			.map(id -> new PersonBasedCalculationExpensePostDTO().id(id).amount(expense.appliedAmount()).approvedAmount(expense.approvedAmount()).note(expense.note()))
			.orElse(null);
	}

	private static PersonBasedCalculationSpecialExpensePostDTO toSpecialExpenseDto(final EffectiveExpense expense, final PersonBasedCalculationProposalDTO proposal) {
		return ExpenseTypeMapper.resolveExpenseTypeId(expense.costType(), proposal, expense.bucket())
			.map(id -> new PersonBasedCalculationSpecialExpensePostDTO().id(id).amount(expense.appliedAmount()).approvedAmount(expense.approvedAmount()).note(expense.note()))
			.orElse(null);
	}

	private static PersonBasedCalculationPersonPostDTO toPersonDto(final EffectivePerson person) {
		return new PersonBasedCalculationPersonPostDTO()
			.personId(person.partyId())
			.numberOfDays(person.numberOfDays())
			.deviationFromDate(toOffsetDateTime(person.deviationFromDate()))
			.deviationToDate(toOffsetDateTime(person.deviationToDate()));
	}

	private static OffsetDateTime toOffsetDateTime(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().atOffset(ZoneOffset.UTC)).orElse(null);
	}

	private static Map<Integer, String> incomeTypeNamesById(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getId() != null) && (type.getName() != null))
			.collect(Collectors.toMap(PersonBasedCalculationCalculationIncomeTypeDTO::getId,
				PersonBasedCalculationCalculationIncomeTypeDTO::getName, (first, second) -> first, LinkedHashMap::new));
	}

	private static DraftRow toDraftRow(final PersonBasedCalculationIncomePostDTO income, final Map<Integer, String> typeNamesById) {
		return new DraftRow(income.getId(), typeNamesById.get(income.getId()),
			income.getApplicantAmount(), toIso(income.getApplicantAmountDate()),
			income.getCoApplicantAmount(), toIso(income.getCoApplicantAmountDate()),
			income.getNote());
	}

	private static PersonBasedCalculationIncomePostDTO toPostDto(final DraftRow row) {
		return new PersonBasedCalculationIncomePostDTO()
			.id(row.typeId())
			.applicantAmount(row.applicantAmount())
			.applicantAmountDate(fromIso(row.applicantAmountDate()))
			.coApplicantAmount(row.coApplicantAmount())
			.coApplicantAmountDate(fromIso(row.coApplicantAmountDate()))
			.note(row.note());
	}

	private static String toIso(final OffsetDateTime date) {
		return ofNullable(date).map(OffsetDateTime::toString).orElse(null);
	}

	private static OffsetDateTime fromIso(final String iso) {
		return ofNullable(iso).filter(StringUtils::hasText).map(OffsetDateTime::parse).orElse(null);
	}

	private List<String> missingPreviousIncomeTypes(final String applicantPersonId, final YearMonth applicationMonth,
		final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		try {
			final var previousTypes = lifecareCaseService.previousCalculationIncomeTypes(applicantPersonId, applicationMonth);
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
