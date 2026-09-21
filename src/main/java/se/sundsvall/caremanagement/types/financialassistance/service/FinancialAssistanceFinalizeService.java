package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.document.service.DocumentService;
import se.sundsvall.caremanagement.journal.service.JournalEntryService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.rpa.service.RpaAction;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Monitoring;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RpaTask;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.REGISTER_PAYMENT;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_DECISION;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_DOCUMENT;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_JOURNAL;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_MONITORING;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_AWAITING_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;
import static se.sundsvall.caremanagement.types.financialassistance.service.MonitoringService.SOURCE_LIFECARE;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.MESSAGE_PAYMENT_DECISION_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.PAYMENT_DECISION_APPROVED;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.PAYMENT_DECISION_REJECTED;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.VARIABLE_PAYMENT_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.DECISION_TYPE_PAYMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toDecisionContent;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toIdListContent;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toPaymentDecision;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toPaymentIdContent;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toPaymentRequest;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toRpaTask;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.updateEntity;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * "Besluta och utbetala" — the caseworker's decision step on a financial assistance (återansökan) errand. Once the
 * three view sections are approved and the errand waits for a decision, one call does, in order:
 *
 * <ol>
 * <li>records the finalize choices on the errand (communication channels, household-size flag);</li>
 * <li>records the decision as a {@code PAYMENT} {@code Decision} row — the audit trail;</li>
 * <li>hands the Lifecare write-backs to RPA (the FamilyCare API has no write endpoints for these): the decision +
 * underrättelse, one item per utbetalning, and the locally authored bevakningar / journal entries / documents;</li>
 * <li>correlates {@code PaymentDecisionReceived} to the waiting process, which then commits the normberäkning to
 * Lifecare, sets the status and polls the payment.</li>
 * </ol>
 *
 * <p>
 * Steps 3 and 4 are best-effort and reported in the response rather than failing the call: the decision is recorded
 * either way, an RPA item that did not get queued can be re-enqueued through the RPA endpoint, and an uncorrelated
 * message re-sent through the process-messages endpoint. {@code WRITE_NORMBERAKNING} is <strong>not</strong> enqueued
 * here — the process's commit step ({@code FinancialAssistanceCalculationService#commitCalculation}) does that, and it
 * reads the household-size flag stored in step 1. Sending the decision to the applicant (step 6 of the verksamhet's
 * flow) is the Draken BFF's job; this service only records and echoes the chosen channels.
 */
@Service
@Transactional
public class FinancialAssistanceFinalizeService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceFinalizeService.class);

	static final String KEY_MONITORING_IDS = "monitoringIds";
	static final String KEY_JOURNAL_ENTRY_IDS = "journalEntryIds";
	static final String KEY_DOCUMENT_IDS = "documentIds";

	private static final String ERROR_NO_TYPED_ERRAND = "No financial-assistance errand for id %s";
	private static final String ERROR_NO_DECIDER = "a decision can only be recorded by an identified user - the X-Sent-By header is required";
	private static final String ERROR_WRONG_STATUS = "errand must be in status %s to be finalized, but is in status '%s'";
	private static final String ERROR_SECTIONS_NOT_APPROVED = "all sections must be approved before the errand can be finalized - not approved: %s";
	private static final String ERROR_ALREADY_FINALIZED = "errand '%s' already carries a %s decision - it has been finalized";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final SectionApprovalService sectionApprovalService;
	private final DecisionService decisionService;
	private final RpaService rpaService;
	private final ProcessService processService;
	private final MonitoringService monitoringService;
	private final JournalEntryService journalEntryService;
	private final DocumentService documentService;
	private final PaymentService paymentService;

	FinancialAssistanceFinalizeService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository,
		final SectionApprovalService sectionApprovalService, final DecisionService decisionService, final RpaService rpaService,
		final ProcessService processService, final MonitoringService monitoringService, final JournalEntryService journalEntryService,
		final DocumentService documentService, final PaymentService paymentService) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.sectionApprovalService = sectionApprovalService;
		this.decisionService = decisionService;
		this.rpaService = rpaService;
		this.processService = processService;
		this.monitoringService = monitoringService;
		this.journalEntryService = journalEntryService;
		this.documentService = documentService;
		this.paymentService = paymentService;
	}

	/**
	 * Finalize the errand — see the class description for the steps. Scoped: {@code 404} when the errand is missing in
	 * this namespace/municipality; {@code 400} without an identified caller; {@code 409} when the errand is not
	 * {@code AWAITING_DECISION}, when a section is not approved, or when a {@code PAYMENT} decision already exists.
	 *
	 * @param  decidedBy the authenticated caseworker (X-Sent-By) — becomes the decision's {@code createdBy}
	 * @return           the receipt: decision id, whether the process was resumed, the per-task RPA outcome, the channels
	 */
	public FinalizeResponse finalize(final String municipalityId, final String namespace, final String errandId, final FinalizeRequest request, final String decidedBy) {
		final var errand = errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		requireDecider(decidedBy);
		requireStatus(errand.getStatus());
		requireApprovedSections(errandId);
		requireNotFinalized(municipalityId, namespace, errandId);
		final var entity = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_NO_TYPED_ERRAND.formatted(errandId)));

		// 1. The audit fields first: the household-size flag must be on the errand before the process (resumed in step 4)
		// runs the commit that enqueues WRITE_NORMBERAKNING.
		financialAssistanceRepository.save(updateEntity(entity, request));

		// 2. The decision row.
		final var decisionId = decisionService.create(municipalityId, namespace, errandId,
			toPaymentDecision(request, decidedBy, LocalDate.now(ZoneId.systemDefault())));

		// 3. The payment rows, before the queue and inside this transaction: a row that fails to save has to roll the
		// decision back with it, rather than leave an errand with a decision and no payments for the robot to find.
		final var paymentIds = createPayments(errandId, request);

		// 4. The Lifecare write-backs, each its own best-effort queue item.
		final var rpaTasks = enqueueWriteBacks(municipalityId, namespace, errandId, request, decisionId, paymentIds);

		// 5. Resume the process.
		final var correlated = correlatePaymentDecision(municipalityId, namespace, errandId, request.getDecision().getOutcome());

		LOG.info("Finalized errand {} with outcome {} (decision {}, process correlated: {})", sanitizeForLogging(errandId),
			sanitizeForLogging(request.getDecision().getOutcome()), sanitizeForLogging(decisionId), correlated);

		return FinalizeResponse.create()
			.withDecisionId(decisionId)
			.withPaymentIds(paymentIds)
			.withProcessMessageCorrelated(correlated)
			.withRpaTasks(rpaTasks)
			.withCommunication(request.getCommunication());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Guards
	// ------------------------------------------------------------------------------------------------------------------

	/** The decision is an audit record of who decided, so an unidentified request is rejected up front. */
	private static void requireDecider(final String decidedBy) {
		if (!hasText(decidedBy)) {
			throw Problem.valueOf(BAD_REQUEST, ERROR_NO_DECIDER);
		}
	}

	/**
	 * Only {@code AWAITING_DECISION} can be finalized: it is the state the daily prepare leaves a complete errand in, and
	 * the only one the status machine lets the process move to GRANTED/REJECTED from. {@code SUPPLEMENT_REQUESTED}
	 * means the SSBTEK picture is still incomplete — the loop has to reach AWAITING_DECISION first.
	 */
	private static void requireStatus(final String status) {
		if (!STATUS_AWAITING_DECISION.equals(status)) {
			throw Problem.valueOf(CONFLICT, ERROR_WRONG_STATUS.formatted(STATUS_AWAITING_DECISION, status));
		}
	}

	/** All three sections (CALCULATION, PAYMENT, DECISION) must be komplett-markerade. */
	private void requireApprovedSections(final String errandId) {
		final var approvals = sectionApprovalService.approvals(errandId);
		final var notApproved = List.of(approvals.getCalculation(), approvals.getPayment(), approvals.getDecision()).stream()
			.filter(approval -> !approval.isApproved())
			.map(SectionApproval::getSection)
			.toList();
		if (!notApproved.isEmpty()) {
			throw Problem.valueOf(CONFLICT, ERROR_SECTIONS_NOT_APPROVED.formatted(String.join(", ", notApproved)));
		}
	}

	/** A second finalize would record a second decision and re-correlate a process that already moved on. */
	private void requireNotFinalized(final String municipalityId, final String namespace, final String errandId) {
		final var alreadyDecided = decisionService.readAll(municipalityId, namespace, errandId).stream()
			.anyMatch(decision -> DECISION_TYPE_PAYMENT.equals(decision.getDecisionType()));
		if (alreadyDecided) {
			throw Problem.valueOf(CONFLICT, ERROR_ALREADY_FINALIZED.formatted(errandId, DECISION_TYPE_PAYMENT));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// RPA write-backs
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * One queue item per Lifecare step: the decision, each payment (with its own reference suffix so the Orchestrator's
	 * dedup does not collapse them), and the bevakningar / journal entries / documents authored here that Lifecare does
	 * not have yet — the latter three only when there is anything to mirror.
	 */
	private List<RpaTask> enqueueWriteBacks(final String municipalityId, final String namespace, final String errandId, final FinalizeRequest request, final String decisionId,
		final List<String> paymentIds) {

		final var tasks = new ArrayList<RpaTask>();
		tasks.add(enqueue(municipalityId, namespace, errandId, WRITE_DECISION, null, toDecisionContent(request, decisionId)));

		// The reference suffix is the payment id rather than a position in the list, so the Orchestrator's dedup keys
		// on the row the item is actually about.
		paymentIds.forEach(paymentId -> tasks.add(
			enqueue(municipalityId, namespace, errandId, REGISTER_PAYMENT, paymentId, toPaymentIdContent(paymentId))));

		enqueueIfAny(tasks, municipalityId, namespace, errandId, WRITE_MONITORING, KEY_MONITORING_IDS, () -> localMonitoringIds(municipalityId, namespace, errandId));
		enqueueIfAny(tasks, municipalityId, namespace, errandId, WRITE_JOURNAL, KEY_JOURNAL_ENTRY_IDS, () -> journalEntryService.listLocallyAuthoredIds(municipalityId, namespace, errandId));
		enqueueIfAny(tasks, municipalityId, namespace, errandId, WRITE_DOCUMENT, KEY_DOCUMENT_IDS, () -> documentService.listLocallyAuthoredIds(municipalityId, namespace, errandId));
		return tasks;
	}

	/**
	 * Persist the decided payments and return their ids, in request order. Each becomes a {@code Payment} row the
	 * robot — and Draken's payment tab — reads through the Payment resource; the queue item carries only the id.
	 */
	private List<String> createPayments(final String errandId, final FinalizeRequest request) {
		return ofNullable(request.getPayments()).orElseGet(List::<FinalizePayment>of).stream()
			.map(payment -> paymentService.createForDecision(errandId, toPaymentRequest(payment)))
			.toList();
	}

	/** The monitorings created or changed in Draken — everything not mirrored from Lifecare. */
	private List<String> localMonitoringIds(final String municipalityId, final String namespace, final String errandId) {
		return monitoringService.list(municipalityId, namespace, errandId).stream()
			.filter(monitoring -> !SOURCE_LIFECARE.equals(monitoring.getSource()))
			.map(Monitoring::getId)
			.toList();
	}

	private void enqueueIfAny(final List<RpaTask> tasks, final String municipalityId, final String namespace, final String errandId, final RpaAction action, final String key,
		final Supplier<List<String>> ids) {
		final var values = ids.get();
		if (!values.isEmpty()) {
			tasks.add(enqueue(municipalityId, namespace, errandId, action, null, toIdListContent(key, values)));
		}
	}

	/**
	 * Enqueue one item, swallowing any failure into the receipt — the decision is already recorded, so a queue hiccup
	 * must not fail the finalize; the caseworker sees {@code enqueued=false} and the step can be re-run via the RPA
	 * endpoint.
	 */
	private RpaTask enqueue(final String municipalityId, final String namespace, final String errandId, final RpaAction action, final String referenceSuffix, final Map<String, String> content) {
		try {
			final var outcome = rpaService.enqueue(municipalityId, namespace, errandId, action, referenceSuffix, content);
			return toRpaTask(action, outcome.reference(), outcome.enqueued());
		} catch (final Exception e) {
			LOG.warn("RPA enqueue {} failed for errand {} — decision already recorded, continuing", sanitizeForLogging(action.name()), sanitizeForLogging(errandId), e);
			return toRpaTask(action, null, false);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Process
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Resume the process waiting at the decision gateway: {@code paymentDecision=APPROVED} for a granting outcome (the
	 * process commits the normberäkning, sets GRANTED and polls the payment), {@code REJECTED} otherwise. The status is
	 * the process's to set, never this service's. Best-effort: an unreachable engine is reported as
	 * {@code processMessageCorrelated=false}, and the message can be re-sent through the process-messages endpoint.
	 */
	private boolean correlatePaymentDecision(final String municipalityId, final String namespace, final String errandId, final String outcome) {
		final String paymentDecision;
		if (outcomeCarriesAmount(outcome)) {
			paymentDecision = PAYMENT_DECISION_APPROVED;
		} else {
			paymentDecision = PAYMENT_DECISION_REJECTED;
		}
		try {
			processService.correlateMessage(municipalityId, namespace, MESSAGE_PAYMENT_DECISION_RECEIVED, errandId, Map.of(VARIABLE_PAYMENT_DECISION, paymentDecision));
			return true;
		} catch (final Exception e) {
			LOG.warn("Could not correlate {} for errand {} — decision recorded, process not resumed", sanitizeForLogging(MESSAGE_PAYMENT_DECISION_RECEIVED), sanitizeForLogging(errandId), e);
			return false;
		}
	}
}
