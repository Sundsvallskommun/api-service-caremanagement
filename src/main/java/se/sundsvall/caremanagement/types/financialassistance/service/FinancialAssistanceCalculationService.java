package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.CalculationService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousFamily;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DayCheckBasis;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.Problem;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_AWAITING_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_SUPPLEMENT_REQUESTED;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The financial-assistance calculation pipeline — preparing the (editable) draft calculation from the
 * process-classified incomes without touching Lifecare ({@link #prepareCalculation}), and the editable draft itself
 * (get/patch header). careM never creates the normberäkning in Lifecare: Draken's BFF owns that write, saves the
 * calculation before the decision (bifall takes its amount from it) and sets its id on the errand as
 * {@code lifecareCalculationId}. The errand envelope, its strongly-typed application data and the case-history reads
 * stay on the per-resource FinancialAssistanceErrandService / FinancialAssistanceLifecareService /
 * FinancialAssistanceActualisationService / FinancialAssistancePaymentService.
 */
@Service
@Transactional
public class FinancialAssistanceCalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceCalculationService.class);

	static final String WARNING_PREVIOUS_NORM_NOT_AVAILABLE = "Normen i föregående normberäkning (%s) finns inte för ansökningsmånaden – normen är vald efter ansökan, kontrollera den";

	/** Decisions recorded by the automated pipelines, written as the drakel system actor. */
	private static final String RECOMMENDATION_TYPE = "RECOMMENDATION";
	private static final String CREATED_BY = "drakel";
	private static final String VALUE_REVIEW_REQUIRED = "REVIEW_REQUIRED";
	private static final String VALUE_OK = "OK";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final CalculationService calculationService;
	private final LifecareCaseService lifecareCaseService;
	private final CitizenService citizenService;
	private final DecisionService decisionService;
	private final WarningService warningService;
	private final DraftService draftService;
	private final CalculationFeeder calculationFeeder;
	private final ApplicationRuleFeeder applicationRuleFeeder;
	private final PeriodRuleFeeder periodRuleFeeder;
	private final MissingIncomeFeeder missingIncomeFeeder;
	private final LateTransferFeeder lateTransferFeeder;

	FinancialAssistanceCalculationService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository, final CalculationService calculationService,
		final LifecareCaseService lifecareCaseService, final CitizenService citizenService, final DecisionService decisionService, final WarningService warningService,
		final DraftService draftService, final CalculationFeeder calculationFeeder, final ApplicationRuleFeeder applicationRuleFeeder, final PeriodRuleFeeder periodRuleFeeder,
		final MissingIncomeFeeder missingIncomeFeeder, final LateTransferFeeder lateTransferFeeder) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.calculationService = calculationService;
		this.lifecareCaseService = lifecareCaseService;
		this.citizenService = citizenService;
		this.decisionService = decisionService;
		this.warningService = warningService;
		this.draftService = draftService;
		this.calculationFeeder = calculationFeeder;
		this.applicationRuleFeeder = applicationRuleFeeder;
		this.periodRuleFeeder = periodRuleFeeder;
		this.missingIncomeFeeder = missingIncomeFeeder;
		this.lateTransferFeeder = lateTransferFeeder;
	}

	/**
	 * Prepare — but do <strong>not</strong> create in Lifecare — the calculation for the application month from incomes
	 * already classified by the operaton rules. The financial assistance process calls this each daily loop: it reports
	 * whether the
	 * information is complete (does this month cover every income type the previous calculation had?), records the income
	 * warnings on the errand as a single {@code Decision(RECOMMENDATION)} the caseworker reviews, and reflects
	 * completeness in the errand status ({@code SUPPLEMENT_REQUESTED} while incomplete, {@code AWAITING_DECISION} when
	 * complete). No Lifecare calculation is created here — careM never creates one; Draken's BFF does.
	 *
	 * <p>
	 * <strong>Once the errand carries a {@code lifecareCalculationId}, the Lifecare normberäkning is the truth.</strong>
	 * The caseworker has saved the calculation in Lifecare from Draken, so careM's draft is no longer refreshed — a
	 * refresh would overwrite nothing the caseworker sees any more, and the warnings it raises would describe a draft
	 * nobody uses. Those draft warnings ({@link WarningService#DRAFT_REFRESH_TYPES}: the new/dropped rows, the expense
	 * feed, the NORM-04 family, the late transfer and the duplicate incomes) are then left exactly as they last were,
	 * neither refreshed nor auto-closed. Everything that works from SSBTEK and the application continues unchanged: the
	 * completeness verdict, the SSBTEK income warnings and the draft-independent rule warnings, the one-time
	 * {@code RECOMMENDATION} decision (on careM's basis — agreed with Draken), the completeness status, the read-failure
	 * warning and the daily-run stamp.
	 */
	public CalculationResponse prepareCalculation(final String municipalityId, final String namespace, final CalculationRequest request) {
		if (TRUE.equals(request.getSsbtekError())) {
			return prepareAfterReadFailure(municipalityId, namespace, request);
		}
		final var input = gather(municipalityId, namespace, request);
		final var previous = previousHousehold(municipalityId, input.applicant(), input.applicationMonth());
		final var refresh = refreshDraftUnlessSavedInLifecare(municipalityId, input, previous);
		final var rules = ruleWarnings(municipalityId, input, previous);
		final var response = completeness(municipalityId, request, input);

		recordRecommendationOnce(municipalityId, namespace, input.errandId(), response);
		refresh.ifPresentOrElse(
			draft -> reconcileWithDraft(input, response, draft, rules),
			() -> reconcileKeepingDraft(input, response, rules));
		// This run read SSBTEK, so any read-failure warning from an earlier run has served its purpose and closes itself.
		warningService.reconcileSsbtekReadFailure(input.errandId(), false);
		applyCompletenessStatus(municipalityId, namespace, input.errandId(), response.isInformationComplete());
		stampDailyRun(input.errand());
		return response;
	}

	/**
	 * The run where SSBTEK could not be read. Verksamhetens regelverk is explicit: do not run the rules, tell the
	 * handläggare a retry is coming, and leave everything else alone. So the draft is not refreshed (an empty income
	 * feed would clear rows the previous run transferred), no recommendation is recorded (it is written once and would
	 * freeze "no warnings" onto an errand we never managed to check) and the status is not touched (an errand waiting
	 * for a decision must not be knocked back to komplettering by a transient outage). Only the warning and the
	 * daily-run stamp happen — the warning closes itself on the next run that succeeds.
	 */
	private CalculationResponse prepareAfterReadFailure(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = request.getErrandId();
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var errand = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No financial-assistance errand for id " + errandId));

		warningService.reconcileSsbtekReadFailure(errandId, true);
		stampDailyRun(errand);

		LOG.warn("SSBTEK could not be read for errand {} — calculation left untouched, read-failure warning raised",
			sanitizeForLogging(errandId));

		// Not complete: a month we could not check must never report itself as checked and done.
		return CalculationResponse.create()
			.withUnhandledIncomes(List.of())
			.withChangeWarnings(List.of())
			.withInformationComplete(false)
			.withMissingIncomeTypes(List.of());
	}

	/**
	 * What one prepare run works from: the request's resolved identifiers plus the errand it targets. The month is kept
	 * both parsed (for the Lifecare reads) and verbatim (the draft header stores the request's own string).
	 */
	private record PrepareInput(String namespace, String errandId, String applicant, YearMonth applicationMonth, String applicationMonthValue,
		String classifiedIncomes, DayCheckBasis dayCheckBasis, FinancialAssistanceEntity errand) {}

	/**
	 * What refreshing the draft produced: the per-row changes to reconcile, and the warnings the refresh raised — the
	 * expense feed, the late comparison-period transfer, the duplicate incomes and the NORM-04 family. All of them are
	 * {@link WarningService#DRAFT_REFRESH_TYPES}.
	 */
	private record DraftRefresh(DraftChanges changes, List<WarningService.WarningInput> expenseWarnings, List<WarningService.WarningInput> lateTransferWarnings,
		List<WarningService.WarningInput> duplicateWarnings, List<WarningService.WarningInput> familyWarnings) {}

	/**
	 * The warnings that do not depend on careM's draft — the housing-cost delta, the återansökan application rules, the
	 * income/household comparisons against the previous normberäkning, the SSBTEK period check and the missing-income
	 * check. Evaluated on every successful run, whether or not the draft is refreshed.
	 */
	private record RuleWarnings(List<WarningService.WarningInput> housingWarnings, List<WarningService.WarningInput> questionWarnings,
		List<WarningService.WarningInput> incomeWarnings, List<WarningService.WarningInput> comparisonWarnings, List<WarningService.WarningInput> periodWarnings,
		List<WarningService.WarningInput> missingIncomeWarnings) {

		List<WarningService.WarningInput> all() {
			return Stream.of(housingWarnings, questionWarnings, incomeWarnings, comparisonWarnings, periodWarnings, missingIncomeWarnings)
				.flatMap(List::stream)
				.toList();
		}
	}

	/**
	 * Resolve everything the run needs before any work is done: the errand is scope-checked (404 outside this
	 * namespace/municipality), the applicant party id resolved to a personal number, and the classified incomes required —
	 * the SSBTEK rules are evaluated in the process, not here.
	 */
	private PrepareInput gather(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = request.getErrandId(); // required + UUID-validated on CalculationRequest (bean validation)
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		// Validated in this order so the caller gets the most specific rejection first: an unresolvable applicant, then a
		// missing classifiedIncomes, then a missing typed errand.
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());
		final var classifiedIncomes = requireClassifiedIncomes(request);
		final var errand = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No financial-assistance errand for id " + errandId));

		return new PrepareInput(namespace, errandId, applicant, applicationMonth, request.getApplicationMonth(), classifiedIncomes, request.getDayCheckBasis(), errand);
	}

	/**
	 * The draft refresh, unless the caseworker has already saved the normberäkning in Lifecare (the errand carries a
	 * {@code lifecareCalculationId}) — from then on the Lifecare calculation is the truth and the draft stays as it is.
	 */
	private Optional<DraftRefresh> refreshDraftUnlessSavedInLifecare(final String municipalityId, final PrepareInput input, final PreviousHousehold previous) {
		if (input.errand().getLifecareCalculationId() != null) {
			LOG.info("Errand {} has a normberäkning saved in Lifecare — the draft is not refreshed", sanitizeForLogging(input.errandId()));
			return Optional.empty();
		}
		return Optional.of(refreshDraft(municipalityId, input, previous));
	}

	/**
	 * Compute the fresh process rows for the three sections, then merge them into the editable draft (the merge keeps the
	 * caseworker's values + soft-deletes; only the process columns are refreshed). The expense feed, the family
	 * comparison, the late transfer and the duplicate check also raise the draft warnings reconciled in
	 * {@link #reconcileWithDraft}.
	 */
	private DraftRefresh refreshDraft(final String municipalityId, final PrepareInput input, final PreviousHousehold previous) {
		final var incomeRows = calculationFeeder.incomeRows(input.errandId(), calculationService.incomeLines(municipalityId, input.applicant(), input.applicationMonth(), input.classifiedIncomes()));
		final var expenseFeed = calculationFeeder.expenseFeed(municipalityId, input.errandId(), input.errand(),
			previousExpenseAmounts(municipalityId, input.applicant(), input.applicationMonth()), ageFromPnr(input.applicant()));
		// NORM-04: norm, familj and gemensamma kostnader come from the previous normberäkning (regelverk återansökan);
		// the application only fills in what FamilyCare's read model lacks, and the rest is flagged.
		final var previousFamily = previousFamily(municipalityId, input.applicant(), input.applicationMonth());
		final var personRows = calculationFeeder.personRows(municipalityId, input.namespace(), input.errandId(), input.errand(),
			previousPersonAmounts(municipalityId, input.applicant(), input.applicationMonth()), previousFamily);
		final var norm = calculationService.selectNormId(municipalityId, input.applicant(), input.applicationMonth(), previousNormNames(previous.norm()),
			normNames(input.errand().getNormType()));
		final var changes = draftService.refresh(input.errandId(), input.applicationMonthValue(), norm.normId(), input.errand().getNormType(),
			personRows, incomeRows, expenseFeed.rows());
		final var familyWarnings = Stream.of(calculationFeeder.familyWarnings(input.errand(), previousFamily),
			calculationFeeder.commonHouseholdCostWarnings(previousFamily, personRows), previousNormWarnings(previous.norm(), norm))
			.flatMap(List::stream)
			.toList();

		// The one rule that both moves money and warns: a comparison-period income last month's calculation never took,
		// which the transfer above has just picked up. The set comes from the transfer's own filter, not a second
		// reading of the rule, so the warning cannot claim something the draft did not do.
		final var lateTransferWarnings = lateTransferFeeder.lateTransferWarnings(
			calculationService.lateTransferredComparisonIncomes(municipalityId, input.applicant(), input.applicationMonth(), input.classifiedIncomes()));
		// Read after the merge, not before: the duplicate only exists once the refreshed process rows sit alongside
		// whatever the caseworker has added by hand.
		final var duplicateWarnings = draftService.duplicateIncomeWarnings(input.errandId());
		return new DraftRefresh(changes, expenseFeed.warnings(), lateTransferWarnings, duplicateWarnings, familyWarnings);
	}

	/**
	 * The warnings that follow from SSBTEK and the application rather than from careM's draft — evaluated on every
	 * successful run, including one where the draft is no longer refreshed.
	 */
	private RuleWarnings ruleWarnings(final String municipalityId, final PrepareInput input, final PreviousHousehold previous) {
		final var housingWarnings = calculationFeeder.housingDeltaWarnings(municipalityId, input.errand(), previous);
		// The verksamhet's återansökan regelverk, evaluated in the engine: the warnings that follow from the answers in
		// the application, the income comparison against the previous normberäkning, and the children/household-count/norm
		// comparisons against it.
		final var questionWarnings = applicationRuleFeeder.applicationQuestionWarnings(municipalityId, input.errandId(), input.errand());
		final var incomeWarnings = applicationRuleFeeder.incomeComparisonWarnings(municipalityId, input.errand(),
			previousIncomeAmounts(municipalityId, input.applicant(), input.applicationMonth()));
		final var comparisonWarnings = applicationRuleFeeder.previousCalculationWarnings(municipalityId, input.errand(), previous);
		// Parsed once and handed to both feeders below.
		final var classifiedIncomes = calculationService.classifiedIncomes(input.classifiedIncomes());
		// The SSBTEK period check (rakel-eb-periodkontroll): the dagersättning day check for aktivitetsstöd/etablerings-/
		// utvecklingsersättning, gated on AF's ekonomiska beslut and FK's 450 days. The control month is the month
		// before the application month - the SSBTEK kontrollperiod.
		final var periodWarnings = periodRuleFeeder.periodWarnings(municipalityId, input.applicationMonth().minusMonths(1), classifiedIncomes,
			input.dayCheckBasis());
		// Verksamhetens "föregående månad = facit": an income SSBTEK reported last month and not this one.
		final var missingIncomeWarnings = missingIncomeFeeder.missingIncomeWarnings(classifiedIncomes);
		return new RuleWarnings(housingWarnings, questionWarnings, incomeWarnings, comparisonWarnings, periodWarnings, missingIncomeWarnings);
	}

	/**
	 * Every warning of a run that refreshed the draft, in the order the reconcile has always received them — the order
	 * new warnings are created in.
	 */
	private static List<WarningService.WarningInput> allWarnings(final DraftRefresh refresh, final RuleWarnings rules) {
		return Stream.of(refresh.expenseWarnings(), rules.housingWarnings(), rules.questionWarnings(), rules.incomeWarnings(),
			rules.comparisonWarnings(), rules.periodWarnings(), rules.missingIncomeWarnings(), refresh.lateTransferWarnings(),
			refresh.duplicateWarnings(), refresh.familyWarnings())
			.flatMap(List::stream)
			.toList();
	}

	/** The verdict the process asked for: does this month cover every income type the previous calculation had? */
	private CalculationResponse completeness(final String municipalityId, final CalculationRequest request, final PrepareInput input) {
		final var completeness = calculationService.completeness(municipalityId, input.applicant(), input.applicationMonth(), input.classifiedIncomes());
		return CalculationResponse.create()
			.withUnhandledIncomes(ofNullable(request.getUnhandledIncomes()).orElseGet(List::of))
			.withChangeWarnings(ofNullable(request.getChangeWarnings()).orElseGet(List::of))
			.withInformationComplete(completeness.informationComplete())
			.withMissingIncomeTypes(completeness.missingIncomeTypes());
	}

	/** Reconcile every calculation warning of a run that refreshed the draft. */
	private void reconcileWithDraft(final PrepareInput input, final CalculationResponse response, final DraftRefresh refresh, final RuleWarnings rules) {
		warningService.reconcileCalculationWarnings(input.errandId(), response.getUnhandledIncomes(), response.getChangeWarnings(),
			response.getMissingIncomeTypes(), refresh.changes(), allWarnings(refresh, rules));
	}

	/**
	 * Reconcile the warnings of a run that did not refresh the draft (the normberäkning is saved in Lifecare): only the
	 * SSBTEK income warnings and the rule warnings — the draft warnings stay as they last were.
	 */
	private void reconcileKeepingDraft(final PrepareInput input, final CalculationResponse response, final RuleWarnings rules) {
		warningService.reconcileRuleWarnings(input.errandId(), response.getUnhandledIncomes(), response.getChangeWarnings(),
			response.getMissingIncomeTypes(), rules.all());
	}

	/** Stamp the errand with this daily-loop run so Draken can show "last checked" and ops can spot stale loops. */
	private void stampDailyRun(final FinancialAssistanceEntity errand) {
		errand.setLastDailyRunAt(OffsetDateTime.now(ZoneId.systemDefault()));
		financialAssistanceRepository.save(errand);
	}

	/** The previous calculation household, best-effort — a failed Lifecare read degrades to "no previous household". */
	private PreviousHousehold previousHousehold(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseService.previousHousehold(municipalityId, applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation household — skipping the household drift check", e);
			return PreviousHousehold.empty();
		}
	}

	/** The previous calculation's family, best-effort — a failed Lifecare read leaves the household to the application. */
	private PreviousFamily previousFamily(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseService.previousFamily(municipalityId, applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation family — the household is taken from the application", e);
			return PreviousFamily.empty();
		}
	}

	/**
	 * The previous calculation's norm as a name to match the month's norms on: FamilyCare names carry the year
	 * (“Riksnorm 2025”) and the month's catalogue has the new one (“Riksnorm 2026”), so the year is dropped.
	 */
	static List<String> previousNormNames(final String previousNorm) {
		return ofNullable(previousNorm)
			.map(norm -> norm.strip().replaceFirst("\\s*\\d{4}$", "").strip())
			.filter(StringUtils::hasText)
			.map(List::of)
			.orElseGet(List::of);
	}

	/** A previous norm the month's catalogue does not offer: the norm was chosen from the application instead. */
	private static List<WarningService.WarningInput> previousNormWarnings(final String previousNorm, final CalculationService.NormChoice norm) {
		if (!StringUtils.hasText(previousNorm) || norm.preferredMatched()) {
			return List.of();
		}
		return List.of(new WarningService.WarningInput(WarningService.TYPE_PREVIOUS_NORM_NOT_AVAILABLE, "previous-norm",
			WARNING_PREVIOUS_NORM_NOT_AVAILABLE.formatted(previousNorm.strip())));
	}

	/** The previous calculation's per-income-type amounts, best-effort — a failed Lifecare read degrades to none. */
	private Map<String, BigDecimal> previousIncomeAmounts(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseService.previousCalculationIncomeAmounts(municipalityId, applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation income amounts — the income comparison is skipped", e);
			return Map.of();
		}
	}

	/**
	 * The previous calculation's per-member norm amounts, best-effort — a failed Lifecare read leaves the Belopp column
	 * empty.
	 */
	private Map<String, BigDecimal> previousPersonAmounts(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseService.previousPersonAmounts(municipalityId, applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation person amounts — the person rows get no amount", e);
			return Map.of();
		}
	}

	/** The previous calculation's per-cost-type approved amounts, best-effort — a failed Lifecare read degrades to none. */
	private Map<String, BigDecimal> previousExpenseAmounts(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseService.previousExpenseAmounts(municipalityId, applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation expense amounts — expense history treated as missing", e);
			return Map.of();
		}
	}

	/** The applicant's age from a Swedish personnummer (YYYYMMDD…), or {@code null} when it cannot be parsed. */
	private static Integer ageFromPnr(final String personalNumber) {
		final var digits = ofNullable(personalNumber).orElse("").replaceAll("\\D", "");
		if (digits.length() < 8) {
			return null;
		}
		try {
			final var birth = LocalDate.parse(digits.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
			return Period.between(birth, LocalDate.now(ZoneId.systemDefault())).getYears();
		} catch (final RuntimeException e) {
			return null;
		}
	}

	/**
	 * The (editable) draft calculation for an errand — the FamilyCare income rows the caseworker reviews and may edit
	 * before a decision. Scoped: throws {@code 404} when the errand (or its draft) is missing.
	 */
	@Transactional(readOnly = true)
	public CalculationDraft getDraft(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return draftService.get(errandId);
	}

	/** Caseworker edit of the draft header — norm, calculation dates and custom household size (common costs). */
	public CalculationDraft patchDraftHeader(final String municipalityId, final String namespace, final String errandId, final NormHeaderInput input) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return draftService.patchHeader(errandId, input);
	}

	private static String requireClassifiedIncomes(final CalculationRequest request) {
		return ofNullable(request.getClassifiedIncomes()).filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "classifiedIncomes is required — the SSBTEK rules is evaluated in the process, not caremanagement"));
	}

	/**
	 * Surface the calculation's income warnings on the errand as a single {@code Decision(RECOMMENDATION)} — written
	 * once (the daily loop re-runs prepare, but the recommendation is not duplicated). The value is {@code
	 * REVIEW_REQUIRED} when there is anything to review (unhandled or significantly changed incomes, or still-missing
	 * SSBTEK data) and {@code OK} otherwise; the description lists the warnings in plain language. It is written on
	 * careM's SSBTEK basis whether or not the caseworker has saved the normberäkning in Lifecare yet, so it is explicitly
	 * preliminary.
	 */
	private void recordRecommendationOnce(final String municipalityId, final String namespace, final String errandId, final CalculationResponse response) {
		final var alreadyRecorded = decisionService.readAll(municipalityId, namespace, errandId).stream()
			.anyMatch(decision -> RECOMMENDATION_TYPE.equals(decision.getDecisionType()));
		if (alreadyRecorded) {
			return;
		}

		final var warnings = Stream.of(
			response.getUnhandledIncomes().stream().map("Ej överförd inkomst: "::concat),
			response.getChangeWarnings().stream().map("Ändrad inkomst: "::concat),
			response.getMissingIncomeTypes().stream().map("Saknas fortfarande i SSBTEK: "::concat))
			.flatMap(stream -> stream)
			.toList();
		final var header = "Inkomstunderlag förberett (preliminärt – normberäkningen sparas i Lifecare av handläggaren). ";
		final String description;
		if (warnings.isEmpty()) {
			description = header + "Inga varningar – inkomsterna kunde överföras utan anmärkning.";
		} else {
			description = header + warnings.size() + " varning(ar) att granska:\n" + String.join("\n", warnings);
		}

		final String value;
		if (warnings.isEmpty()) {
			value = VALUE_OK;
		} else {
			value = VALUE_REVIEW_REQUIRED;
		}
		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(RECOMMENDATION_TYPE)
			.withValue(value)
			.withDescription(description)
			.withCreatedBy(CREATED_BY));
	}

	/**
	 * Reflect SSBTEK completeness in the errand status — {@code SUPPLEMENT_REQUESTED} while incomplete, {@code
	 * AWAITING_DECISION} when complete — writing only when it actually changes (the daily loop re-runs prepare, so an
	 * unchanged status is a no-op).
	 */
	private void applyCompletenessStatus(final String municipalityId, final String namespace, final String errandId, final boolean informationComplete) {
		final String target;
		if (informationComplete) {
			target = STATUS_AWAITING_DECISION;
		} else {
			target = STATUS_SUPPLEMENT_REQUESTED;
		}
		final var current = errandService.readErrand(municipalityId, namespace, errandId).getStatus();
		if (!target.equals(current)) {
			errandService.updateErrand(municipalityId, namespace, errandId, PatchErrand.create().withStatus(target));
		}
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}

	/**
	 * The application's norm types as the Swedish names FamilyCare's norm catalogue uses. The translation lives here
	 * because {@code normType} is this errand type's vocabulary: the lifecare module serves every errand type and has
	 * no business knowing what {@code NATIONAL_NORM} means, which is also what the module cycle check enforces.
	 */
	private static List<String> normNames(final List<String> normTypes) {
		return ofNullable(normTypes).orElseGet(List::of).stream()
			.map(FinancialAssistanceLabels::normTypeDisplayName)
			.filter(StringUtils::hasText)
			.toList();
	}
}
