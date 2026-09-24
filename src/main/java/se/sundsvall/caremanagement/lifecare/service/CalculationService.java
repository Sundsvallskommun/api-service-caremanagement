package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.caremanagement.lifecare.service.mapper.CalculationAssembler;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFamilyCareMapper;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Prepares the calculation rows for careM's draft from incomes already classified by the operaton rules — read-only
 * towards Lifecare FamilyCare. caremanagement neither fetches SSBTEK (the rules live in the process) nor creates the
 * normberäkning in Lifecare (Draken's BFF owns that write). This service resolves each classified income's category to
 * a FamilyCare income-type id (via {@link ClassifiedIncomeToFamilyCareMapper}), chooses the norm for the month against
 * the applicant's proposal (via {@link CalculationAssembler}), and reports whether this month's incomes cover every
 * income type the previous month's calculation had — the financial assistance process polls SSBTEK daily until they
 * do.
 */
@Service
public class CalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(CalculationService.class);

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
	 * Whether this month's classified incomes cover every income type the previous calculation had. Best-effort: a failure
	 * reading the previous month is treated as complete so the financial assistance process is not wedged.
	 */
	public Completeness completeness(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		final var missing = missingPreviousIncomeTypes(municipalityId, applicantPersonId, applicationMonth, parse(classifiedIncomesJson), proposal);
		return new Completeness(missing.isEmpty(), missing);
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
	 * {@link CalculationAssembler#matchingNormId}) and otherwise the first of {@code fallbackNames} found among the
	 * norms whose window covers the month ({@link CalculationAssembler#selectNormId}). One proposal read either way.
	 */
	public NormChoice selectNormId(final String municipalityId, final String applicantPersonId, final YearMonth applicationMonth,
		final List<String> preferredNames, final List<String> fallbackNames) {
		final var proposal = lifecareFamilyCareIntegration.getCalculationProposal(municipalityId, applicantPersonId);
		return CalculationAssembler.matchingNormId(proposal, applicationMonth, preferredNames)
			.map(normId -> new NormChoice(normId, true))
			.orElseGet(() -> new NormChoice(CalculationAssembler.selectNormId(proposal, applicationMonth, fallbackNames).orElse(null), false));
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
