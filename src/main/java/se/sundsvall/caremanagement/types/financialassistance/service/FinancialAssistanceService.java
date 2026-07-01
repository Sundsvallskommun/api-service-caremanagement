package se.sundsvall.caremanagement.types.financialassistance.service;

import java.io.IOException;
import java.math.BigDecimal;
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
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Actualisation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocument;
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
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.LifecareHistoryMapper;
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
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Creates and reads financial-assistance errands. The envelope is owned by the exposed core {@link ErrandService};
 * the {@code typeSlug} is one of the three financial assistance slugs (new / renewal / supplementary) and the stored
 * {@code applicationType}
 * is derived from it server-side, so the slug stays authoritative. Initial status is {@code RECEIVED}. The
 * strongly-typed
 * application data lives on this module's own table, keyed by the envelope id. The title falls back to the financial
 * assistance display
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

	/** How far back the actualisation listing reaches when the caller gives no explicit {@code from} date. */
	private static final int ACTUALISATION_LOOKBACK_MONTHS = 24;
	/** Lifecare archive defaults — used when the archive request omits the matching field; all overridable per request. */
	private static final String DEFAULT_ARCHIVE_DOCUMENT_TYPE = "ANSOKAN";
	private static final String DEFAULT_ARCHIVE_DOCUMENT_SENDER_TYPE = "ENSKILD";
	private static final String DEFAULT_ARCHIVE_SENDER_NAME = "Draken";
	private static final String DOCUMENT_NOT_FOUND_MESSAGE = "No Lifecare document '%s' found for the given applicant";
	private static final String ACTUALISATION_NOT_FOUND_MESSAGE = "No Lifecare actualisation '%s' found for the given applicant";
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
	private final LifecareCaseHistoryService lifecareCaseHistoryService;

	FinancialAssistanceService(final ErrandService errandService, final FinancialAssistanceRepository repository, final CalculationService calculationService,
		final ActualisationService actualisationService, final PaymentStatusService paymentStatusService, final CitizenService citizenService, final DecisionService decisionService,
		final AttachmentService attachmentService, final StakeholderService stakeholderService, final WarningService warningService, final SectionApprovalService sectionApprovalService,
		final DraftService draftService, final CalculationFeeder calculationFeeder, final RpaService rpaService, final FormSnapshotService formSnapshotService,
		final LifecareCaseHistoryService lifecareCaseHistoryService) {
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
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
	}

	/**
	 * Create the financial assistance errand, persist its strongly-typed data, promote the application's persons
	 * (applicant/co-applicant) to
	 * core stakeholder rows on the errand, and — when the citizen supplied supporting files — store each attachment plus a
	 * single combined PDF merging them all. Attachments are type-agnostic: the list is handed to the attachments module
	 * as-is. The optional {@code caseData} file is the application snapshot (case data): it is stored as a single
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

		final var errandId = errandService.createTypedErrand(municipalityId, namespace, envelope);

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
	 * already classified by the operaton rules. The financial assistance process calls this each daily loop: it reports
	 * whether the
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
		final var sokandeAlder = ageFromPnr(applicant);
		final var expenseFeed = calculationFeeder.expenseFeed(municipalityId, errandId, errand,
			previousExpenseAmounts(applicant, applicationMonth), sokandeAlder);
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
		repository.save(errand);
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

	/** The previous calculation's per-cost-type approved amounts, best-effort — a failed Lifecare read degrades to none. */
	private Map<String, Double> previousExpenseAmounts(final String applicant, final YearMonth applicationMonth) {
		try {
			return calculationService.previousExpenseAmounts(applicant, applicationMonth);
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
	 * The financial assistance income warnings on an errand — the acknowledgeable objects a caseworker reviews in Draken.
	 * Scoped: throws
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
	 * The caseworker approval state of the three financial assistance view sections (calculation / payment / decision).
	 * Scoped: throws
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
	 *
	 * <p>
	 * {@code namespace} is currently unused by the commit itself (the errand is resolved by id and the Lifecare write is
	 * keyed on personnummer), but is kept to match the uniform {@code (municipalityId, namespace, request)} signature the
	 * {@link se.sundsvall.caremanagement.types.financialassistance.api.FinancialAssistanceResource} passes for every
	 * scoped endpoint — and so a later scope check can be added without changing the controller contract.
	 */
	@SuppressWarnings("java:S1172") // namespace retained for controller-facing signature symmetry; see Javadoc
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

		// The calculation is now in Lifecare via the FC API; ask RPA to mirror the rest of the decision surface that has no
		// FC endpoint. Best-effort — the Lifecare write already succeeded, so a queue hiccup must not fail the commit.
		triggerRpaWrite(municipalityId, errandId, WRITE_NORMBERAKNING);

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
	 *
	 * <p>
	 * {@code namespace} is currently unused by the commit itself (see {@link #commitCalculation}); it is kept for the same
	 * controller-facing signature symmetry.
	 */
	@SuppressWarnings("java:S1172") // namespace retained for controller-facing signature symmetry; see Javadoc
	public CalculationResponse commitFromApplication(final String municipalityId, final String namespace, final CalculationRequest request) {
		final var errandId = requireErrandId(request);
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());
		final var errand = repository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No financial-assistance errand for id " + errandId));

		// Incomes straight from the application, but folded + converted through the same pipeline as the SSBTEK path
		// (CalculationFeeder.incomeRows → toEffectiveIncome); expenses + persons via the same feeder, already application-
		// sourced. Nothing here is calculation logic of its own — only the application's data fed into the existing engine.
		final var incomeLines = calculationService.applicationIncomeLines(applicant, toApplicationIncomes(errand.getIncomes()));
		final var incomes = calculationFeeder.incomeRows(errandId, incomeLines).stream()
			.map(FinancialAssistanceService::toEffectiveIncome).toList();
		final var expenses = calculationFeeder.applicationExpenseRows(errandId, errand).stream()
			.map(FinancialAssistanceService::toEffectiveExpense).toList();
		final var persons = calculationFeeder.personRows(errandId, errand).stream()
			.map(FinancialAssistanceService::toEffectivePerson).toList();

		final var normId = calculationService.selectNormId(applicant, applicationMonth);
		final var header = new CalculationHeader(normId, applicationMonth.atDay(1), applicationMonth.atEndOfMonth(), LocalDate.now(ZoneId.systemDefault()), false, null);

		final var calculationId = calculationService.commitEffective(applicant, applicationMonth, header, incomes, expenses, persons);
		triggerRpaWrite(municipalityId, errandId, WRITE_NORMBERAKNING);

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

	/** Enqueue an RPA write, swallowing any failure — RPA mirroring must never roll back a successful Lifecare write. */
	private void triggerRpaWrite(final String municipalityId, final String errandId, final String action) {
		try {
			rpaService.enqueue(municipalityId, errandId, action);
		} catch (final Exception e) {
			LOG.warn("RPA enqueue {} failed for errand {} — Lifecare write already committed, continuing", sanitizeForLogging(action), sanitizeForLogging(errandId), e);
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
			response.getUnhandledIncomes().stream().map("Ej överförd inkomst: "::concat),
			response.getChangeWarnings().stream().map("Ändrad inkomst: "::concat),
			response.getMissingIncomeTypes().stream().map("Saknas fortfarande i SSBTEK: "::concat))
			.flatMap(stream -> stream)
			.toList();
		final var header = "Inkomstunderlag förberett (preliminärt – normberäkningen skapas i Lifecare efter beslut). ";
		final var description = warnings.isEmpty()
			? header + "Inga varningar – inkomsterna kunde överföras utan anmärkning."
			: header + warnings.size() + " varning(ar) att granska:\n" + String.join("\n", warnings);

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
	 * caseworker when one was found (the same caseworker set on the Lifecare actualisation).
	 */
	private void recordActualisation(final String municipalityId, final String namespace, final String errandId, final ActualisationResult result) {
		addActualisationDecision(municipalityId, namespace, errandId, result.actualisationId(),
			"Actualisation created in Lifecare (id %d).".formatted(result.actualisationId()));

		ofNullable(result.assignedUserId()).filter(StringUtils::hasText)
			.ifPresent(assignedUserId -> errandService.updateErrand(municipalityId, namespace, errandId,
				PatchErrand.create().withAssignedUserId(assignedUserId)));
	}

	/**
	 * Set the errand's Lifecare actualisation to the given id by recording the canonical {@code Decision(ACTUALISATION)}.
	 */
	private void addActualisationDecision(final String municipalityId, final String namespace, final String errandId, final Integer actualisationId, final String description) {
		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(ACTUALISATION_TYPE)
			.withValue(String.valueOf(actualisationId))
			.withDescription(description)
			.withCreatedBy(CREATED_BY));
	}

	/**
	 * List the Lifecare actualisations (case intakes) registered on the applicant, so a caseworker can pick which one a
	 * supplementary application is archived to. The applicant is identified by partyId (resolved to a
	 * personnummer via the citizen service — 404 when unknown). The period defaults to the last
	 * {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when {@code from}/{@code to} are omitted.
	 */
	@Transactional(readOnly = true)
	public List<Actualisation> listActualisations(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var applicant = personalNumber(municipalityId, partyId);
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return actualisationService.list(applicant, fromDate, toDate).stream()
			.map(FinancialAssistanceService::toActualisation)
			.toList();
	}

	/**
	 * List the applicant's Lifecare calculations — the full case-history read the frontend renders
	 * straight from Lifecare. The applicant is identified by partyId (resolved to a personnummer via the citizen service —
	 * 404 when unknown). The period defaults to the last {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when
	 * {@code from}/{@code to} are omitted.
	 */
	@Transactional(readOnly = true)
	public List<LifecareCalculation> listCalculations(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var applicant = personalNumber(municipalityId, partyId);
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return lifecareCaseHistoryService.listCalculations(applicant, fromDate, toDate).stream()
			.map(LifecareHistoryMapper::toCalculation)
			.toList();
	}

	/**
	 * List the applicant's Lifecare decisions — served straight from Lifecare. The applicant is identified by
	 * partyId (resolved to a personnummer via the citizen service — 404 when unknown). The period defaults to the last
	 * {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when {@code from}/{@code to} are omitted.
	 */
	@Transactional(readOnly = true)
	public List<LifecareDecision> listDecisions(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var applicant = personalNumber(municipalityId, partyId);
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return lifecareCaseHistoryService.listDecisions(applicant, fromDate, toDate).stream()
			.map(LifecareHistoryMapper::toDecision)
			.toList();
	}

	/**
	 * List the applicant's Lifecare documents (metadata) — served straight from Lifecare. The applicant is identified by
	 * partyId (resolved to a personnummer via the citizen service — 404 when unknown). The period defaults to the last
	 * {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when {@code from}/{@code to} are omitted. The content of a
	 * single document is fetched via {@link #readDocumentContent(String)}.
	 */
	@Transactional(readOnly = true)
	public List<LifecareDocument> listDocuments(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var applicant = personalNumber(municipalityId, partyId);
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return lifecareCaseHistoryService.listDocuments(applicant, fromDate, toDate).stream()
			.map(LifecareHistoryMapper::toDocument)
			.toList();
	}

	/**
	 * Read a single Lifecare document's content (the generated PDF) — the bytes are streamed straight from Lifecare,
	 * caremanagement only forwards them. Gated on ownership: the document id must belong to the given applicant (the same
	 * partyId scope as {@link #listDocuments}), so a caller cannot read another applicant's document by guessing its
	 * Lifecare-global id. A document id not in the applicant's list for the period yields 404 before any bytes are read.
	 */
	@Transactional(readOnly = true)
	public byte[] readDocumentContent(final String municipalityId, final String partyId, final String documentId, final LocalDate from, final LocalDate to) {
		final var owned = listDocuments(municipalityId, partyId, from, to).stream()
			.anyMatch(document -> documentId.equals(document.getId()));
		if (!owned) {
			throw Problem.valueOf(NOT_FOUND, DOCUMENT_NOT_FOUND_MESSAGE.formatted(documentId));
		}
		return lifecareCaseHistoryService.documentContent(documentId);
	}

	/**
	 * Archive an uploaded document (e.g. a supplementary application) to a specific Lifecare
	 * actualisation by binding it as an attachment. caremanagement only forwards the bytes — the file is supplied by the
	 * frontend. Document type / sender type / sender name fall back to server defaults when the request omits them; the
	 * title defaults to the uploaded file name. When the request carries an {@code errandId}, the target actualisation id
	 * is recorded on that errand as a {@code Decision(ACTUALISATION)} — setting the errand's Lifecare actualisation to the
	 * one archived to.
	 *
	 * <p>
	 * Gated on ownership: the actualisation id must belong to the given applicant (the same partyId scope as
	 * {@link #listActualisations}), so a caller cannot bind a file to another applicant's actualisation by guessing its
	 * (sequential) Lifecare-global id — a foreign id yields 404 before anything is uploaded.
	 */
	public void archiveToActualisation(final String municipalityId, final String namespace, final String partyId, final Integer actualisationId, final MultipartFile file, final ArchiveActualisationRequest request) {
		final var owned = listActualisations(municipalityId, partyId, null, null).stream()
			.anyMatch(actualisation -> actualisationId.equals(actualisation.getId()));
		if (!owned) {
			throw Problem.valueOf(NOT_FOUND, ACTUALISATION_NOT_FOUND_MESSAGE.formatted(actualisationId));
		}

		final var meta = ofNullable(request).orElseGet(ArchiveActualisationRequest::create);
		final var fileName = ofNullable(file.getOriginalFilename()).filter(StringUtils::hasText).orElse("dokument.pdf");
		final var title = ofNullable(meta.getTitle()).filter(StringUtils::hasText).orElse(fileName);
		final var documentType = ofNullable(meta.getDocumentType()).filter(StringUtils::hasText).orElse(DEFAULT_ARCHIVE_DOCUMENT_TYPE);
		final var documentSenderType = ofNullable(meta.getDocumentSenderType()).filter(StringUtils::hasText).orElse(DEFAULT_ARCHIVE_DOCUMENT_SENDER_TYPE);
		final var senderName = ofNullable(meta.getSenderName()).filter(StringUtils::hasText).orElse(DEFAULT_ARCHIVE_SENDER_NAME);

		actualisationService.uploadAttachment(actualisationId, fileName, readBytes(file), documentType, documentSenderType, title, senderName);

		ofNullable(meta.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> addActualisationDecision(municipalityId, namespace, errandId, actualisationId,
				"Actualisation set on errand from archive (id %d).".formatted(actualisationId)));
	}

	/** Read the uploaded file's bytes, surfacing an unreadable upload as a 400 rather than an opaque 500. */
	private static byte[] readBytes(final MultipartFile file) {
		try {
			return file.getBytes();
		} catch (final IOException e) {
			throw Problem.valueOf(BAD_REQUEST, "Could not read the uploaded file: " + e.getMessage());
		}
	}

	/** Project the lifecare-module summary onto the API model. */
	private static Actualisation toActualisation(final ActualisationSummary summary) {
		return Actualisation.create()
			.withId(summary.id())
			.withType(summary.type())
			.withName(summary.name())
			.withDate(summary.date())
			.withReason(summary.reason())
			.withRegards(summary.regards())
			.withFromWho(summary.fromWho())
			.withCaseworker(summary.caseworker())
			.withOrganization(summary.organization())
			.withStatus(summary.status())
			.withInvestigationId(summary.investigationId())
			.withServiceId(summary.serviceId())
			.withDecisionId(summary.decisionId());
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
