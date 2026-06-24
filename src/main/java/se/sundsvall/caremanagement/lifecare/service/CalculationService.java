package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationSpecialExpensePostDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ApplicationIncomeToFcMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.CalculationAssembler;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFcMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationDraftBuild;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationResult;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.DraftRow;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper.BUCKET_SPECIAL_EXPENSE;

/**
 * Builds and posts the calculation to Lifecare FC from incomes already classified by the operaton regelverk.
 * caremanagement no longer fetches SSBTEK or evaluates the raw list — the regelverk lives in the process. This service
 * resolves each classified income's category to an FC income-type id (via {@link ClassifiedIncomeToFcMapper}),
 * assembles
 * the calculation against the applicant's proposal (via {@link CalculationAssembler}), and posts it. It also reports
 * whether this month's calculation covers every income type the previous month's did — the EB process polls SSBTEK
 * daily until it does.
 */
@Service
public class CalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(CalculationService.class);

	private final LifecareFcIntegration lifecareFcIntegration;
	private final LifecareEbCaseService lifecareEbCaseService;
	private final ObjectMapper objectMapper;

	public CalculationService(final LifecareFcIntegration lifecareFcIntegration, final LifecareEbCaseService lifecareEbCaseService,
		final ObjectMapper objectMapper) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.lifecareEbCaseService = lifecareEbCaseService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Build and post the calculation from incomes already classified by the operaton regelverk, and report whether the
	 * information is complete — i.e. whether every income type on the previous month's calculation is present this
	 * month. The completeness lookup is best-effort: a failure reading the previous calculation never fails the just-
	 * created calculation; it is reported as complete (with no missing types) so the process is not wedged.
	 *
	 * @param  applicantPersonId     the applicant's personnummer (the FC proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the created calculation id plus the completeness verdict + missing income types
	 */
	public CalculationResult buildAndPostFromClassified(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFcMapper.toCalculationIncomes(classified, proposal);
		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		final var calculationId = lifecareFcIntegration.createCalculation(body);

		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new CalculationResult(calculationId, missing.isEmpty(), missing);
	}

	/**
	 * Build the draft calculation from the classified incomes — the FC income rows plus the completeness verdict —
	 * <strong>without</strong> creating anything in Lifecare. The EB process stores the rows as an editable draft and
	 * polls SSBTEK daily until the information is complete; the Lifecare calculation is created later, on a decision, from
	 * the (possibly edited) draft (see {@link #postDraftRows}).
	 *
	 * @param  applicantPersonId     the applicant's personnummer (the FC proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the FC income rows + completeness verdict; nothing is written to Lifecare
	 */
	public CalculationDraftBuild buildDraft(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFcMapper.toCalculationIncomes(classified, proposal);
		final var typeNamesById = incomeTypeNamesById(proposal);
		final var rows = incomes.stream().map(income -> toDraftRow(income, typeNamesById)).toList();
		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new CalculationDraftBuild(rows, missing.isEmpty(), missing);
	}

	/**
	 * Create the calculation in Lifecare FC from the (possibly caseworker-edited) draft income rows — called on a
	 * decision. Assembles against the applicant's proposal and posts; returns the created calculation id.
	 */
	public Integer postDraftRows(final String applicantPersonId, final YearMonth applicationMonth, final List<DraftRow> rows) {
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ofNullable(rows).orElseGet(List::of).stream().map(CalculationService::toPostDto).toList();
		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		return lifecareFcIntegration.createCalculation(body);
	}

	/**
	 * The process-derived income lines for the draft — one per (FC income type, recipient) — from the operaton-classified
	 * incomes resolved against the applicant's calculation proposal. Writes nothing to Lifecare.
	 */
	public List<FcIncomeLine> incomeLines(final String applicantPersonId, final String classifiedIncomesJson) {
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		return ClassifiedIncomeToFcMapper.toIncomeLines(parse(classifiedIncomesJson), proposal);
	}

	/**
	 * The process-derived income lines for a calculation built straight from the incomes the citizen declared in the
	 * application — the nyansökan sibling of {@link #incomeLines}, no SSBTEK. Each application income code is translated to
	 * its FC income type and resolved against the applicant's calculation proposal (via
	 * {@link ApplicationIncomeToFcMapper});
	 * incomes whose type does not resolve are skipped. Same {@link FcIncomeLine} shape as the SSBTEK path, so the
	 * downstream fold + commit pipeline is shared. Writes nothing to Lifecare.
	 */
	public List<FcIncomeLine> applicationIncomeLines(final String applicantPersonId, final List<ApplicationIncome> incomes) {
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		return ApplicationIncomeToFcMapper.toIncomeLines(incomes, proposal);
	}

	/**
	 * Whether this month's classified incomes cover every income type the previous calculation had. Best-effort: a
	 * failure reading the previous month is treated as complete so the EB process is not wedged.
	 */
	public Completeness completeness(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, parse(classifiedIncomesJson), proposal);
		return new Completeness(missing.isEmpty(), missing);
	}

	/** The household on the applicant's previous calculation in Lifecare — the baseline for the household drift check. */
	public PreviousHousehold previousHousehold(final String applicantPersonId, final YearMonth applicationMonth) {
		return lifecareEbCaseService.previousHousehold(applicantPersonId, applicationMonth);
	}

	/** The norm id the FC proposal offers for the application month (covering window, else first), or {@code null}. */
	public Integer selectNormId(final String applicantPersonId, final YearMonth applicationMonth) {
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		return CalculationAssembler.selectNormId(proposal, applicationMonth).orElse(null);
	}

	/**
	 * Create the calculation in Lifecare FC from the draft's effective rows — called on a decision. Folds the effective
	 * incomes, expenses (resolving each cost type to an FC expense-type id, skipping the unresolvable) and household
	 * persons into the FC body, overriding the proposal norm with the one chosen on the draft, and posts it.
	 *
	 * @return the created Lifecare calculation id
	 */
	public Integer commitEffective(final String applicantPersonId, final YearMonth applicationMonth, final CalculationHeader header,
		final List<EffectiveIncome> incomes, final List<EffectiveExpense> expenses, final List<EffectivePerson> persons) {

		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomeDtos = ofNullable(incomes).orElseGet(List::of).stream().map(CalculationService::toIncomeDto).toList();

		final var allExpenses = ofNullable(expenses).orElseGet(List::of);
		final var expenseDtos = allExpenses.stream()
			.filter(expense -> !BUCKET_SPECIAL_EXPENSE.equals(expense.bucket()))
			.map(expense -> toExpenseDto(expense, proposal)).filter(Objects::nonNull).toList();
		final var specialExpenseDtos = allExpenses.stream()
			.filter(expense -> BUCKET_SPECIAL_EXPENSE.equals(expense.bucket()))
			.map(expense -> toSpecialExpenseDto(expense, proposal)).filter(Objects::nonNull).toList();

		final var personDtos = ofNullable(persons).orElseGet(List::of).stream().map(CalculationService::toPersonDto).toList();

		final var body = CalculationAssembler.assemble(applicantPersonId, proposal, incomeDtos, expenseDtos, specialExpenseDtos, personDtos, header, applicationMonth);
		return lifecareFcIntegration.createCalculation(body);
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
			.collect(java.util.stream.Collectors.toMap(PersonBasedCalculationCalculationIncomeTypeDTO::getId,
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
			final var previousTypes = lifecareEbCaseService.previousCalculationIncomeTypes(applicantPersonId, applicationMonth);
			return ClassifiedIncomeToFcMapper.missingPreviousIncomeTypes(previousTypes, classified, proposal);
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
