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
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ApplicationIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.CalculationAssembler;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper;
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
	 * The process-derived income lines for the draft — one per (FamilyCare income type, recipient) — from the
	 * operaton-classified incomes resolved against the applicant's calculation proposal. Writes nothing to Lifecare.
	 */
	public List<FamilyCareIncomeLine> incomeLines(final String applicantPersonId, final String classifiedIncomesJson) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		return ClassifiedIncomeToFamilyCareMapper.toIncomeLines(parse(classifiedIncomesJson), proposal);
	}

	/**
	 * The process-derived income lines for a calculation built straight from the incomes the citizen declared in the
	 * application — the new application sibling of {@link #incomeLines}, no SSBTEK. Each application income code is
	 * translated to its FamilyCare income type and resolved against the applicant's calculation proposal (via {@link
	 * ApplicationIncomeToFamilyCareMapper}); incomes whose type does not resolve are skipped. Same {@link
	 * FamilyCareIncomeLine} shape as the SSBTEK path, so the downstream fold + commit pipeline is shared. Writes nothing
	 * to Lifecare.
	 */
	public List<FamilyCareIncomeLine> applicationIncomeLines(final String applicantPersonId, final List<ApplicationIncome> incomes) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(applicantPersonId);
		return ApplicationIncomeToFamilyCareMapper.toIncomeLines(incomes, proposal);
	}

	/**
	 * Whether this month's classified incomes cover every income type the previous calculation had. Best-effort: a failure
	 * reading the previous month is treated as complete so the financial assistance process is not wedged.
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
	 * effective incomes, expenses (resolving each cost type to a FamilyCare expense-type id, skipping the unresolvable)
	 * and household persons into the FamilyCare body, overriding the proposal norm with the one chosen on the draft, and
	 * posts it.
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
			.applicantAmount(toWireAmount(income.applicantAmount()))
			.applicantAmountDate(income.applicantAmountDate())
			.coApplicantAmount(toWireAmount(income.coApplicantAmount()))
			.coApplicantAmountDate(income.coApplicantAmountDate())
			.note(income.note());
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
