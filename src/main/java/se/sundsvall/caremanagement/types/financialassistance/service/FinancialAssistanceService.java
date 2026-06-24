package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.service.FormSnapshotService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.CalculationService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_NORMBERAKNING;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_AWAITING_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_SUPPLEMENT_REQUESTED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.applicationTypeForSlug;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toEntity;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toStakeholders;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toView;

/**
 * Creates and reads financial-assistance (EB) errands. The envelope is owned by the exposed core {@link ErrandService};
 * the {@code typeSlug} is one of the three EB slugs (new / renewal / supplementary) and the stored
 * {@code applicationType}
 * is derived from it server-side, so the slug stays authoritative. Initial status is {@code RECEIVED}. The
 * strongly-typed
 * application data lives on this module's own table, keyed by the envelope id. The title falls back to the EB display
 * name when the client omits one.
 */
@Service
@Transactional
public class FinancialAssistanceService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceService.class);

	private static final String DEFAULT_TITLE = "Financial assistance";

	/** Decisions recorded by the automated pipelines, written as the drakel system actor. */
	private static final String RECOMMENDATION_TYPE = "RECOMMENDATION";
	private static final String ACTUALISATION_TYPE = "ACTUALISATION";
	private static final String CREATED_BY = "drakel";
	private static final String VALUE_REVIEW_REQUIRED = "REVIEW_REQUIRED";
	private static final String VALUE_OK = "OK";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository repository;
	private final CalculationService calculationService;
	private final ActualisationService actualisationService;
	private final PaymentStatusService paymentStatusService;
	private final CitizenService citizenService;
	private final DecisionService decisionService;
	private final AttachmentService attachmentService;
	private final StakeholderService stakeholderService;
	private final WarningService warningService;
	private final SectionApprovalService sectionApprovalService;
	private final DraftService draftService;
	private final CalculationFeeder calculationFeeder;
	private final RpaService rpaService;
	private final FormSnapshotService formSnapshotService;

	FinancialAssistanceService(final ErrandService errandService, final FinancialAssistanceRepository repository, final CalculationService calculationService,
		final ActualisationService actualisationService, final PaymentStatusService paymentStatusService, final CitizenService citizenService, final DecisionService decisionService,
		final AttachmentService attachmentService, final StakeholderService stakeholderService, final WarningService warningService, final SectionApprovalService sectionApprovalService,
		final DraftService draftService, final CalculationFeeder calculationFeeder, final RpaService rpaService, final FormSnapshotService formSnapshotService) {
		this.errandService = errandService;
		this.repository = repository;
		this.calculationService = calculationService;
		this.actualisationService = actualisationService;
		this.paymentStatusService = paymentStatusService;
		this.citizenService = citizenService;
		this.decisionService = decisionService;
		this.attachmentService = attachmentService;
		this.stakeholderService = stakeholderService;
		this.warningService = warningService;
		this.sectionApprovalService = sectionApprovalService;
		this.draftService = draftService;
		this.calculationFeeder = calculationFeeder;
		this.rpaService = rpaService;
		this.formSnapshotService = formSnapshotService;
	}

	/**
	 * Create the EB errand, persist its strongly-typed data, promote the application's persons (applicant/co-applicant) to
	 * core stakeholder rows on the errand, and — when the citizen supplied supporting files — store each attachment plus a
	 * single combined PDF merging them all. Attachments are type-agnostic: the list is handed to the attachments module
	 * as-is. The optional {@code caseData} file is the application snapshot (ärendeuppgifter): it is stored as a single
	 * {@code CASE_DATA} attachment, renamed to {@code {errandNumber}.pdf}, so the whole errand is created in one call.
	 * The optional {@code formSnapshot} is the self-describing JSON snapshot of the form as the applicant saw it (every
	 * question, help/info/notice text, option label and answer): it is captured write-once for the legal record.
	 */
	public String create(final String municipalityId, final String namespace, final String typeSlug, final CreateFinancialAssistanceRequest request,
		final List<MultipartFile> attachments, final MultipartFile caseData, final String formSnapshot) {
		final var envelope = Errand.create()
			.withTypeSlug(typeSlug)
			.withTitle(ofNullable(request.getTitle()).orElse(DEFAULT_TITLE))
			.withStatus(STATUS_RECEIVED)
			.withDescription(request.getDescription())
			.withPriority(request.getPriority())
			.withReporterUserId(request.getReporterUserId())
			.withAssignedUserId(request.getAssignedUserId());

		final var errandId = errandService.createErrand(municipalityId, namespace, envelope);

		final var entity = ofNullable(toEntity(request.getData(), errandId))
			.orElseGet(() -> FinancialAssistanceEntity.create().withErrandId(errandId));
		entity.setApplicationType(applicationTypeForSlug(typeSlug)); // slug is authoritative — overrides any client-sent value
		repository.save(entity);

		// Promote the application's persons to stakeholders so the errand carries its applicant/co-applicant in the shared
		// collection.
		toStakeholders(ofNullable(request.getData()).map(FinancialAssistanceData::getPersons).orElse(null))
			.forEach(stakeholder -> stakeholderService.create(municipalityId, namespace, errandId, stakeholder));

		ofNullable(attachments)
			.filter(files -> !files.isEmpty())
			.ifPresent(files -> attachmentService.storeAndCombine(municipalityId, namespace, errandId, files));

		ofNullable(caseData)
			.ifPresent(file -> attachmentService.createCaseDataAttachment(municipalityId, namespace, errandId, file));

		ofNullable(formSnapshot)
			.filter(StringUtils::hasText)
			.ifPresent(payload -> formSnapshotService.capture(municipalityId, namespace, errandId, typeSlug, payload));

		return errandId;
	}

	/**
	 * The immutable form snapshot of an errand — the citizen-facing application form as it was rendered and answered, for
	 * re-display. Scoped: throws {@code 404} when the errand is missing in this namespace/municipality, or when no
	 * snapshot was captured.
	 */
	@Transactional(readOnly = true)
	public FormSnapshot readFormSnapshot(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return formSnapshotService.read(errandId);
	}

	@Transactional(readOnly = true)
	public FinancialAssistanceView read(final String municipalityId, final String namespace, final String errandId) {
		final var envelope = errandService.readErrand(municipalityId, namespace, errandId);
		final var entity = repository.findByErrandId(errandId).orElse(null);
		return toView(envelope, entity)
			.withRecommendation(latestRecommendation(municipalityId, namespace, errandId))
			.withSectionApprovals(sectionApprovalService.approvals(errandId));
	}

	/** The most recent {@code RECOMMENDATION} decision on the errand (the automated recommendation), or null when none. */
	private Decision latestRecommendation(final String municipalityId, final String namespace, final String errandId) {
		return decisionService.readAll(municipalityId, namespace, errandId).stream()
			.filter(decision -> RECOMMENDATION_TYPE.equals(decision.getDecisionType()))
			.findFirst()
			.orElse(null);
	}

	public void updateData(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceData data) {
		// Scope check — throws 404 when the errand is missing in this namespace/municipality.
		errandService.readErrand(municipalityId, namespace, errandId);
		repository.save(toEntity(data, errandId));
	}

	/**
	 * Prepare — but do <strong>not</strong> create in Lifecare — the calculation for the application month from incomes
	 * already classified by the operaton rules. The EB process calls this each daily loop: it reports whether the
	 * information is complete (does this month cover every income type the previous calculation had?), records the income
	 * warnings on the errand as a single {@code Decision(RECOMMENDATION)} the caseworker reviews, and reflects
	 * completeness in the errand status ({@code SUPPLEMENT_REQUESTED} while incomplete, {@code AWAITING_DECISION} when
	 * complete).
	 * No Lifecare calculation is created here — that happens only after a decision, via {@link #commitCalculation}.
	 */
	public CalculationResponse prepareCalculation(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = requireErrandId(request);
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());
		final var classifiedIncomes = requireClassifiedIncomes(request);
		final var errand = repository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No financial-assistance errand for id " + errandId));

		// Compute the fresh process rows for the three sections, then merge them into the editable draft (the merge keeps
		// the caseworker's values + soft-deletes; only the process columns are refreshed).
		final var incomeRows = calculationFeeder.incomeRows(errandId, calculationService.incomeLines(applicant, classifiedIncomes));
		final var expenseFeed = calculationFeeder.expenseFeed(municipalityId, errandId, errand);
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
		return response;
	}

	/** The previous calculation household, best-effort — a failed Lifecare read degrades to "no previous household". */
	private PreviousHousehold previousHousehold(final String applicant, final YearMonth applicationMonth) {
		try {
			return calculationService.previousHousehold(applicant, applicationMonth);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation household — skipping the household drift check", e);
			return PreviousHousehold.empty();
		}
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

	// --- per-row caseworker edits on the draft (scoped to the errand; touch only caseworker columns / soft-delete) ---

	public NormIncomeRow addDraftIncome(final String municipalityId, final String namespace, final String errandId, final NormIncomeInput input) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return draftService.addIncome(errandId, input);
	}

	public NormIncomeRow patchDraftIncome(final String municipalityId, final String namespace, final String errandId, final String rowId, final NormIncomeInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.patchIncome(errandId, rowId, input);
	}

	public NormIncomeRow setDraftIncomeDeleted(final String municipalityId, final String namespace, final String errandId, final String rowId, final boolean deleted) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.setIncomeDeleted(errandId, rowId, deleted);
	}

	public NormExpenseRow addDraftExpense(final String municipalityId, final String namespace, final String errandId, final NormExpenseInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.addExpense(errandId, input);
	}

	public NormExpenseRow patchDraftExpense(final String municipalityId, final String namespace, final String errandId, final String rowId, final NormExpenseInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.patchExpense(errandId, rowId, input);
	}

	public NormExpenseRow setDraftExpenseDeleted(final String municipalityId, final String namespace, final String errandId, final String rowId, final boolean deleted) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.setExpenseDeleted(errandId, rowId, deleted);
	}

	public NormPersonRow addDraftPerson(final String municipalityId, final String namespace, final String errandId, final NormPersonInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.addPerson(errandId, input);
	}

	public NormPersonRow patchDraftPerson(final String municipalityId, final String namespace, final String errandId, final String rowId, final NormPersonInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.patchPerson(errandId, rowId, input);
	}

	public NormPersonRow setDraftPersonDeleted(final String municipalityId, final String namespace, final String errandId, final String rowId, final boolean deleted) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.setPersonDeleted(errandId, rowId, deleted);
	}

	/**
	 * Create a warning on an errand directly — the careM temp stage, with no Lifecare round-trip. Scoped: throws
	 * {@code 404} when the errand is missing in this namespace/municipality.
	 */
	public Warning createWarning(final String municipalityId, final String namespace, final String errandId, final CreateWarningRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.create(errandId, request.getType(), request.getSourceKey(), request.getMessage());
	}

	/**
	 * The EB income warnings on an errand — the acknowledgeable objects a caseworker reviews in Draken. Scoped: throws
	 * {@code 404} when the errand is missing in this namespace/municipality.
	 */
	@Transactional(readOnly = true)
	public List<Warning> listWarnings(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.list(errandId);
	}

	/**
	 * How many active (OPEN/ACKNOWLEDGED, not CLOSED) income warnings are on an errand — the badge count. Scoped: throws
	 * {@code 404} when the errand is missing in this namespace/municipality.
	 */
	@Transactional(readOnly = true)
	public long countActiveWarnings(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.countActive(errandId);
	}

	/**
	 * Acknowledge or close a warning on an errand. Scoped: throws {@code 404} when the errand or warning is missing,
	 * {@code 400} when the target status is not {@code ACKNOWLEDGED}/{@code CLOSED}.
	 */
	public Warning updateWarning(final String municipalityId, final String namespace, final String errandId, final String warningId, final String status) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.updateStatus(errandId, warningId, status);
	}

	/**
	 * The caseworker approval state of the three EB view sections (calculation / payment / decision). Scoped: throws
	 * {@code 404} when the errand is missing in this namespace/municipality.
	 */
	@Transactional(readOnly = true)
	public SectionApprovals getSectionApprovals(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return sectionApprovalService.approvals(errandId);
	}

	/**
	 * Set a section's approval — a caseworker verifies it as approved (or withdraws the approval). Scoped: throws
	 * {@code 404} when the errand is missing, {@code 400} when the section is not CALCULATION/PAYMENT/DECISION.
	 */
	public SectionApproval setSectionApproval(final String municipalityId, final String namespace, final String errandId, final String section,
		final boolean approved, final String approvedBy) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return sectionApprovalService.setApproval(errandId, section, approved, approvedBy);
	}

	/**
	 * Create the calculation in Lifecare FC from the classified incomes — called once a decision is taken, never during
	 * the daily SSBTEK loop. Returns the created calculation id (plus the completeness verdict for reference).
	 */
	public CalculationResponse commitCalculation(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = requireErrandId(request);
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());

		// Post the (possibly caseworker-edited) draft to Lifecare: the effective value of each live row, soft-deleted rows
		// skipped.
		final var header = draftService.header(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No draft calculation to commit for errand " + errandId));
		final var incomes = draftService.liveIncomes(errandId).stream().map(FinancialAssistanceService::toEffectiveIncome).toList();
		final var expenses = draftService.liveExpenses(errandId).stream().map(FinancialAssistanceService::toEffectiveExpense).toList();
		final var persons = draftService.livePersons(errandId).stream().map(FinancialAssistanceService::toEffectivePerson).toList();

		final var calculationHeader = new CalculationHeader(header.getNormId(), header.getCalculationFromDate(), header.getCalculationToDate(),
			header.getCalculationDate(), header.getHasCustomHouseholdSize(), header.getHouseholdSize());
		final var calculationId = calculationService.commitEffective(applicant, applicationMonth, calculationHeader, incomes, expenses, persons);

		// The normberäkning is now in Lifecare via the FC API; ask RPA to mirror the rest of the beslut surface that has no
		// FC endpoint. Best-effort — the Lifecare write already succeeded, so a queue hiccup must not fail the commit.
		triggerRpaWrite(municipalityId, errandId, WRITE_NORMBERAKNING);

		return CalculationResponse.create()
			.withCalculationId(calculationId)
			.withUnhandledIncomes(ofNullable(request.getUnhandledIncomes()).orElseGet(List::of))
			.withChangeWarnings(ofNullable(request.getChangeWarnings()).orElseGet(List::of));
	}

	/** Enqueue an RPA write, swallowing any failure — RPA mirroring must never roll back a successful Lifecare write. */
	private void triggerRpaWrite(final String municipalityId, final String errandId, final String action) {
		try {
			rpaService.enqueue(municipalityId, errandId, action);
		} catch (final Exception e) {
			LOG.warn("RPA enqueue {} failed for errand {} — Lifecare write already committed, continuing", action, errandId, e);
		}
	}

	/** One live income row → its effective FC income (applicant + co-applicant effective amounts), ready to post. */
	private static EffectiveIncome toEffectiveIncome(final FaNormIncomeEntity row) {
		return new EffectiveIncome(row.getTypeId(),
			effectiveDouble(row.getApplicantCaseworkerAmount(), row.getApplicantProcessAmount()), row.getApplicantAmountDate(),
			effectiveDouble(row.getCoapplicantCaseworkerAmount(), row.getCoapplicantProcessAmount()), row.getCoapplicantAmountDate(),
			row.getNote());
	}

	private static EffectiveExpense toEffectiveExpense(final FaNormExpenseEntity row) {
		return new EffectiveExpense(row.getCostType(), row.getBucket(),
			ofNullable(row.getAppliedAmount()).map(BigDecimal::doubleValue).orElse(null),
			effectiveDouble(row.getCaseworkerAmount(), row.getProcessAmount()),
			row.getNote());
	}

	private static EffectivePerson toEffectivePerson(final FaNormPersonEntity row) {
		return new EffectivePerson(row.getPartyId(), DraftService.effectiveDays(row.getCaseworkerDays(), row.getProcessDays()),
			row.getDeviationFromDate(), row.getDeviationToDate());
	}

	private static Double effectiveDouble(final BigDecimal caseworkerAmount, final BigDecimal processAmount) {
		return ofNullable(DraftService.effectiveAmount(caseworkerAmount, processAmount)).map(BigDecimal::doubleValue).orElse(null);
	}

	private static String requireErrandId(final CalculationRequest request) {
		return ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "errandId is required for the calculation"));
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
			response.getUnhandledIncomes().stream().map("Not transferred income: "::concat),
			response.getChangeWarnings().stream().map("Changed income: "::concat),
			response.getMissingIncomeTypes().stream().map("Still missing in SSBTEK: "::concat))
			.flatMap(stream -> stream)
			.toList();
		final var header = "Income basis prepared (preliminary - the calculation is created in Lifecare after a decision). ";
		final var description = warnings.isEmpty()
			? header + "No warnings - the incomes could be transferred without remarks."
			: header + warnings.size() + " varning(ar) att granska:\n" + String.join("\n", warnings);

		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(RECOMMENDATION_TYPE)
			.withValue(warnings.isEmpty() ? VALUE_OK : VALUE_REVIEW_REQUIRED)
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
		final var target = informationComplete ? STATUS_AWAITING_DECISION : STATUS_SUPPLEMENT_REQUESTED;
		final var current = errandService.readErrand(municipalityId, namespace, errandId).getStatus();
		if (!target.equals(current)) {
			errandService.updateErrand(municipalityId, namespace, errandId, PatchErrand.create().withStatus(target));
		}
	}

	/**
	 * Create the Lifecare actualisation (case intake) for the application month and return the created actualisation id.
	 * The intake date is the first day of the application month. When the request carries an {@code errandId}, the
	 * creation is recorded on that errand as a {@code Decision(ACTUALISATION)} so the caseworker sees it in the case's
	 * audit trail.
	 */
	public ActualisationResponse createActualisation(final String municipalityId, final String namespace, final ActualisationRequest request) {
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var intakeDate = YearMonth.parse(request.getApplicationMonth()).atDay(1);
		final var result = actualisationService.create(applicant, intakeDate);

		ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> recordActualisation(municipalityId, namespace, errandId, result));

		return ActualisationResponse.create().withActualisationId(result.actualisationId());
	}

	/**
	 * Read whether the manual Lifecare payment for the application month has been effectuated for the applicant.
	 * caremanagement makes no payment — the caseworker does it in Lifecare; the process polls this to detect when the
	 * payment is registered. Returns the effectuated flag and, when effectuated, the Lifecare PayDate.
	 */
	public PaymentStatusResponse checkPaymentStatus(final String municipalityId, final PaymentStatusRequest request) {
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final PaymentStatus status = paymentStatusService.read(applicant, YearMonth.parse(request.getApplicationMonth()));
		return PaymentStatusResponse.create()
			.withEffectuated(status.effectuated())
			.withPaymentDate(status.paymentDate());
	}

	/**
	 * Record the created actualisation on the errand as a {@code Decision(ACTUALISATION)} — the canonical audit-trail
	 * vehicle on the case — carrying the Lifecare actualisation id as the value, and assign the errand to the resolved
	 * handläggare when one was found (the same caseworker set on the Lifecare actualisation).
	 */
	private void recordActualisation(final String municipalityId, final String namespace, final String errandId, final ActualisationResult result) {
		final var actualisationId = result.actualisationId();
		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(ACTUALISATION_TYPE)
			.withValue(String.valueOf(actualisationId))
			.withDescription("Actualisation created in Lifecare (id %d).".formatted(actualisationId))
			.withCreatedBy(CREATED_BY));

		ofNullable(result.assignedUserId()).filter(StringUtils::hasText)
			.ifPresent(assignedUserId -> errandService.updateErrand(municipalityId, namespace, errandId,
				PatchErrand.create().withAssignedUserId(assignedUserId)));
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
