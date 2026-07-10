package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.CalculationDraftMapper;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_NORMBERAKNING;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_AWAITING_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_SUPPLEMENT_REQUESTED;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The financial-assistance calculation pipeline — preparing the (editable) draft calculation from the
 * process-classified
 * incomes without touching Lifecare ({@link #prepareCalculation}) and, once a decision is taken, committing the
 * effective
 * draft to Lifecare FC ({@link #commitCalculation}). This service also owns the editable draft (get/patch header) and
 * the from-application commit; the errand envelope, its strongly-typed application data and the case-history reads stay
 * on the per-resource FinancialAssistanceErrandService / FinancialAssistanceLifecareService /
 * FinancialAssistanceActualisationService / FinancialAssistancePaymentService.
 */
@Service
@Transactional
public class FinancialAssistanceCalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceCalculationService.class);

	/** Decisions recorded by the automated pipelines, written as the drakel system actor. */
	private static final String RECOMMENDATION_TYPE = "RECOMMENDATION";
	private static final String CREATED_BY = "drakel";
	private static final String VALUE_REVIEW_REQUIRED = "REVIEW_REQUIRED";
	private static final String VALUE_OK = "OK";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final CalculationService calculationService;
	private final LifecareEbCaseService lifecareEbCaseService;
	private final CitizenService citizenService;
	private final DecisionService decisionService;
	private final WarningService warningService;
	private final DraftService draftService;
	private final CalculationFeeder calculationFeeder;
	private final RpaService rpaService;

	FinancialAssistanceCalculationService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository, final CalculationService calculationService,
		final LifecareEbCaseService lifecareEbCaseService, final CitizenService citizenService, final DecisionService decisionService, final WarningService warningService,
		final DraftService draftService, final CalculationFeeder calculationFeeder, final RpaService rpaService) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.calculationService = calculationService;
		this.lifecareEbCaseService = lifecareEbCaseService;
		this.citizenService = citizenService;
		this.decisionService = decisionService;
		this.warningService = warningService;
		this.draftService = draftService;
		this.calculationFeeder = calculationFeeder;
		this.rpaService = rpaService;
	}

	/**
	 * Prepare — but do <strong>not</strong> create in Lifecare — the calculation for the application month from incomes
	 * already classified by the operaton rules. The financial assistance process calls this each daily loop: it reports
	 * whether the
	 * information is complete (does this month cover every income type the previous calculation had?), records the income
	 * warnings on the errand as a single {@code Decision(RECOMMENDATION)} the caseworker reviews, and reflects
	 * completeness in the errand status ({@code SUPPLEMENT_REQUESTED} while incomplete, {@code AWAITING_DECISION} when
	 * complete).
	 * No Lifecare calculation is created here — that happens only after a decision, via {@link #commitCalculation}.
	 */
	public CalculationResponse prepareCalculation(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = request.getErrandId(); // required + UUID-validated on CalculationRequest (bean validation)
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());
		final var classifiedIncomes = requireClassifiedIncomes(request);
		final var errand = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No financial-assistance errand for id " + errandId));

		// Compute the fresh process rows for the three sections, then merge them into the editable draft (the merge keeps
		// the caseworker's values + soft-deletes; only the process columns are refreshed).
		final var incomeRows = calculationFeeder.incomeRows(errandId, calculationService.incomeLines(applicant, classifiedIncomes));
		final var applicantAge = ageFromPnr(applicant);
		final var expenseFeed = calculationFeeder.expenseFeed(municipalityId, errandId, errand,
			previousExpenseAmounts(applicant, applicationMonth), applicantAge);
		final var personRows = calculationFeeder.personRows(errandId, errand);
		final var normId = calculationService.selectNormId(applicant, applicationMonth);
		final var draftChanges = draftService.refresh(errandId, request.getApplicationMonth(), normId, errand.getNormType(), personRows, incomeRows, expenseFeed.rows());

		final var completeness = calculationService.completeness(applicant, applicationMonth, classifiedIncomes);
		final var deltaWarnings = calculationFeeder.householdDeltaWarnings(municipalityId, errand, personRows, previousHousehold(applicant, applicationMonth));

		final var response = CalculationResponse.create()
			.withUnhandledIncomes(ofNullable(request.getUnhandledIncomes()).orElseGet(List::of))
			.withChangeWarnings(ofNullable(request.getChangeWarnings()).orElseGet(List::of))
			.withInformationComplete(completeness.informationComplete())
			.withMissingIncomeTypes(completeness.missingIncomeTypes());

		recordRecommendationOnce(municipalityId, namespace, errandId, response);
		final var sectionWarnings = Stream.concat(expenseFeed.warnings().stream(), deltaWarnings.stream()).toList();
		warningService.reconcileCalculationWarnings(errandId, response.getUnhandledIncomes(), response.getChangeWarnings(),
			response.getMissingIncomeTypes(), draftChanges, sectionWarnings);
		applyCompletenessStatus(municipalityId, namespace, errandId, completeness.informationComplete());

		// Stamp the errand with this daily-loop run so Draken can show "last checked" and ops can spot stale loops.
		errand.setLastDailyRunAt(OffsetDateTime.now(ZoneId.systemDefault()));
		financialAssistanceRepository.save(errand);
		return response;
	}

	/** The previous calculation household, best-effort — a failed Lifecare read degrades to "no previous household". */
	private PreviousHousehold previousHousehold(final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareEbCaseService.previousHousehold(applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation household — skipping the household drift check", e);
			return PreviousHousehold.empty();
		}
	}

	/** The previous calculation's per-cost-type approved amounts, best-effort — a failed Lifecare read degrades to none. */
	private Map<String, Double> previousExpenseAmounts(final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareEbCaseService.previousExpenseAmounts(applicant, applicationMonth);
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
	 * Create the calculation in Lifecare FC from the classified incomes — called once a decision is taken, never during
	 * the daily SSBTEK loop. Returns the created calculation id (plus the completeness verdict for reference).
	 */
	public CalculationResponse commitCalculation(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = request.getErrandId(); // required + UUID-validated on CalculationRequest (bean validation)
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());

		// Post the (possibly caseworker-edited) draft to Lifecare: the effective value of each live row, soft-deleted rows
		// skipped.
		final var header = draftService.header(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No draft calculation to commit for errand " + errandId));
		final var incomes = draftService.liveIncomes(errandId).stream().map(CalculationDraftMapper::toEffectiveIncome).toList();
		final var expenses = draftService.liveExpenses(errandId).stream().map(CalculationDraftMapper::toEffectiveExpense).toList();
		final var persons = draftService.livePersons(errandId).stream().map(CalculationDraftMapper::toEffectivePerson).toList();

		final var calculationHeader = new CalculationHeader(header.getNormId(), header.getCalculationFromDate(), header.getCalculationToDate(),
			header.getCalculationDate(), header.getHasCustomHouseholdSize(), header.getHouseholdSize());
		final var calculationId = calculationService.commitEffective(applicant, applicationMonth, calculationHeader, incomes, expenses, persons);

		// The calculation is now in Lifecare via the FC API; ask RPA to mirror the rest of the decision surface that has no
		// FC endpoint. Best-effort — the Lifecare write already succeeded, so a queue hiccup must not fail the commit.
		triggerRpaWrite(municipalityId, errandId);

		return CalculationResponse.create()
			.withCalculationId(calculationId)
			.withUnhandledIncomes(ofNullable(request.getUnhandledIncomes()).orElseGet(List::of))
			.withChangeWarnings(ofNullable(request.getChangeWarnings()).orElseGet(List::of));
	}

	/**
	 * Create the calculation in Lifecare FC straight from the incomes, costs and household the citizen declared in the
	 * application — the new application path: no SSBTEK, no daily loop, no caseworker draft. Incomes come from the
	 * application's
	 * own declared incomes (resolved to FC types by name), expenses and persons from the same feeder the renewal path uses
	 * (both already application-sourced), and the norm from the proposal for the application month. Posts in one shot and
	 * returns the created calculation id.
	 */
	public CalculationResponse commitFromApplication(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = request.getErrandId(); // required + UUID-validated on CalculationRequest (bean validation)
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());
		final var errand = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No financial-assistance errand for id " + errandId));

		// Incomes straight from the application, but folded + converted through the same pipeline as the SSBTEK path
		// (CalculationFeeder.incomeRows → toEffectiveIncome); expenses + persons via the same feeder, already application-
		// sourced. Nothing here is calculation logic of its own — only the application's data fed into the existing engine.
		final var incomeLines = calculationService.applicationIncomeLines(applicant, toApplicationIncomes(errand.getIncomes()));
		final var incomes = calculationFeeder.incomeRows(errandId, incomeLines).stream()
			.map(CalculationDraftMapper::toEffectiveIncome).toList();
		final var expenses = calculationFeeder.applicationExpenseRows(errandId, errand).stream()
			.map(CalculationDraftMapper::toEffectiveExpense).toList();
		final var persons = calculationFeeder.personRows(errandId, errand).stream()
			.map(CalculationDraftMapper::toEffectivePerson).toList();

		final var normId = calculationService.selectNormId(applicant, applicationMonth);
		final var header = new CalculationHeader(normId, applicationMonth.atDay(1), applicationMonth.atEndOfMonth(), LocalDate.now(ZoneId.systemDefault()), false, null);

		final var calculationId = calculationService.commitEffective(applicant, applicationMonth, header, incomes, expenses, persons);
		triggerRpaWrite(municipalityId, errandId);

		return CalculationResponse.create().withCalculationId(calculationId);
	}

	/** The application's declared incomes as the neutral {@link ApplicationIncome} the FC mapper consumes. */
	private static List<ApplicationIncome> toApplicationIncomes(final List<FaIncome> incomes) {
		return ofNullable(incomes).orElseGet(List::of).stream()
			.map(income -> new ApplicationIncome(income.getIncomeType(), income.getAmount(), income.getIncomeDate(), toRole(income.getRecipient())))
			.toList();
	}

	/** Map the application recipient code to a role — anything but the explicit co-applicant code is the applicant. */
	private static ApplicantRole toRole(final String recipient) {
		if (ApplicantRole.CO_APPLICANT.name().equals(recipient)) {
			return ApplicantRole.CO_APPLICANT;
		}
		return ApplicantRole.APPLICANT;
	}

	/**
	 * The (editable) draft calculation for an errand — the FC income rows the caseworker reviews and may edit before a
	 * decision. Scoped: throws {@code 404} when the errand (or its draft) is missing.
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

	/** Enqueue an RPA write, swallowing any failure — RPA mirroring must never roll back a successful Lifecare write. */
	private void triggerRpaWrite(final String municipalityId, final String errandId) {
		try {
			rpaService.enqueue(municipalityId, errandId, WRITE_NORMBERAKNING);
		} catch (final Exception e) {
			LOG.warn("RPA enqueue {} failed for errand {} — Lifecare write already committed, continuing", sanitizeForLogging(WRITE_NORMBERAKNING.name()), sanitizeForLogging(errandId), e);
		}
	}

	private static String requireClassifiedIncomes(final CalculationRequest request) {
		return ofNullable(request.getClassifiedIncomes()).filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "classifiedIncomes is required — the SSBTEK rules is evaluated in the process, not caremanagement"));
	}

	/**
	 * Surface the calculation's income warnings on the errand as a single {@code Decision(RECOMMENDATION)} — written
	 * once (the daily loop re-runs prepare, but the recommendation is not duplicated). The value is {@code REVIEW_REQUIRED}
	 * when there is anything to review (unhandled or significantly changed incomes, or still-missing SSBTEK data) and
	 * {@code OK} otherwise; the description lists the warnings in plain language. No Lifecare calculation exists yet, so
	 * the recommendation is explicitly preliminary.
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
		final var header = "Inkomstunderlag förberett (preliminärt – normberäkningen skapas i Lifecare efter beslut). ";
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
	 * Reflect SSBTEK completeness in the errand status — {@code SUPPLEMENT_REQUESTED} while incomplete,
	 * {@code AWAITING_DECISION}
	 * when complete — writing only when it actually changes (the daily loop re-runs prepare, so an unchanged status is a
	 * no-op).
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
}
