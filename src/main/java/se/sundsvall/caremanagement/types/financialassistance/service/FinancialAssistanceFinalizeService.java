package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_AWAITING_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.MESSAGE_PAYMENT_DECISION_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.PAYMENT_DECISION_APPROVED;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.PAYMENT_DECISION_REJECTED;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceProcessMessages.VARIABLE_PAYMENT_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.DECISION_TYPE_PAYMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toPaymentDecision;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.toPaymentRequest;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.updateEntity;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * "Besluta och utbetala" — the caseworker's decision step on a financial assistance (återansökan) errand. Once the
 * three view sections are approved and the errand waits for a decision, one call does, in order:
 *
 * <ol>
 * <li>records the finalize choices on the errand (communication channels, household-size flag);</li>
 * <li>records the decision as a {@code PAYMENT} {@code Decision} row — the audit trail;</li>
 * <li>records one {@code Payment} row per decided utbetalning, which Draken's BFF registers directly in Lifecare and
 * acknowledges through {@code .../payments/{paymentId}/lifecare-result};</li>
 * <li>correlates {@code PaymentDecisionReceived} to the waiting process, which then commits the normberäkning to
 * Lifecare, sets the status and polls the payment.</li>
 * </ol>
 *
 * <p>
 * Step 4 is best-effort and reported in the response rather than failing the call: the decision is recorded either
 * way, and an uncorrelated message is queued, in this transaction, for the scheduled process-message retry. The
 * Lifecare writes — decision, utbetalningar, bevakningar, journal, documents — are all the Draken BFF's, done directly
 * against Lifecare; nothing is queued for a robot (RPA was retired 2026-09-24). Sending the decision to the applicant
 * (step 6 of the verksamhet's flow) is the BFF's job too; this service only records and echoes the chosen channels.
 */
@Service
@Transactional
public class FinancialAssistanceFinalizeService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceFinalizeService.class);

	private static final String ERROR_NO_TYPED_ERRAND = "No financial-assistance errand for id %s";
	private static final String ERROR_NO_DECIDER = "a decision can only be recorded by an identified user - the X-Sent-By header is required";
	private static final String ERROR_WRONG_STATUS = "errand must be in status %s to be finalized, but is in status '%s'";
	private static final String ERROR_SECTIONS_NOT_APPROVED = "all sections must be approved before the errand can be finalized - not approved: %s";
	private static final String ERROR_ALREADY_FINALIZED = "errand '%s' already carries a %s decision - it has been finalized";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final SectionApprovalService sectionApprovalService;
	private final DecisionService decisionService;
	private final ProcessService processService;
	private final PaymentService paymentService;
	private final PayeeService payeeService;

	FinancialAssistanceFinalizeService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository,
		final SectionApprovalService sectionApprovalService, final DecisionService decisionService,
		final ProcessService processService, final PaymentService paymentService, final PayeeService payeeService) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.sectionApprovalService = sectionApprovalService;
		this.decisionService = decisionService;
		this.processService = processService;
		this.paymentService = paymentService;
		this.payeeService = payeeService;
	}

	/**
	 * Finalize the errand — see the class description for the steps. Scoped: {@code 404} when the errand is missing in
	 * this namespace/municipality; {@code 400} without an identified caller; {@code 409} when the errand is not
	 * {@code AWAITING_DECISION}, when a section is not approved, or when a {@code PAYMENT} decision already exists.
	 *
	 * @param  decidedBy the authenticated caseworker (X-Sent-By) — becomes the decision's {@code createdBy}
	 * @return           the receipt: decision id, payment ids, whether the process was resumed, the channels
	 */
	public FinalizeResponse finalize(final String municipalityId, final String namespace, final String errandId, final FinalizeRequest request, final String decidedBy) {
		final var errand = errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		requireDecider(decidedBy);
		requireStatus(errand.getStatus());
		requireApprovedSections(errandId);
		requireNotFinalized(municipalityId, namespace, errandId);
		final var entity = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_NO_TYPED_ERRAND.formatted(errandId)));

		// 1. The audit fields first (communication channels, household-size flag), so they are on the errand before the
		// process is resumed in step 4.
		financialAssistanceRepository.save(updateEntity(entity, request));

		// 2. The decision row.
		final var decisionId = decisionService.create(municipalityId, namespace, errandId,
			toPaymentDecision(request, decidedBy, LocalDate.now(ZoneId.systemDefault())));

		// 3. The payment rows, inside this transaction: a row that fails to save has to roll the decision back with it,
		// rather than leave an errand with a decision and no payments for the BFF to register.
		final var paymentIds = createPayments(errandId, request);

		// 3b. A payment cannot be registered in Lifecare against a payee that is not there yet, so the receipt says which
		// of the decided payees careM has not seen reported SYNCED. Deliberately a warning and not a guard: the decision is
		// the caseworker's, and refusing here would strand an otherwise complete decision.
		final var payeeWarnings = payeeService.unsyncedPayeeWarnings(errandId, decidedPayees(request));
		payeeWarnings.forEach(warning -> LOG.warn("Errand {} finalized with a payee that is not in Lifecare yet: {}",
			sanitizeForLogging(errandId), sanitizeForLogging(warning)));

		// 4. Resume the process.
		final var correlated = correlatePaymentDecision(municipalityId, namespace, errandId, request.getDecision().getOutcome());

		LOG.info("Finalized errand {} with outcome {} (decision {}, process correlated: {})", sanitizeForLogging(errandId),
			sanitizeForLogging(request.getDecision().getOutcome()), sanitizeForLogging(decisionId), correlated);

		return FinalizeResponse.create()
			.withDecisionId(decisionId)
			.withPaymentIds(paymentIds)
			.withProcessMessageCorrelated(correlated)
			.withCommunication(request.getCommunication())
			.withPayeeWarnings(payeeWarnings);
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
	// Payments
	// ------------------------------------------------------------------------------------------------------------------

	/** The payees the decision actually pays to, in request order — what the payee warnings are matched against. */
	private static List<Payee> decidedPayees(final FinalizeRequest request) {
		return ofNullable(request.getPayments()).orElseGet(List::<FinalizePayment>of).stream()
			.map(FinalizePayment::getPayee)
			.toList();
	}

	/**
	 * Persist the decided payments and return their ids, in request order. Each becomes a {@code Payment} row Draken's
	 * BFF registers in Lifecare and acknowledges back.
	 */
	private List<String> createPayments(final String errandId, final FinalizeRequest request) {
		return ofNullable(request.getPayments()).orElseGet(List::<FinalizePayment>of).stream()
			.map(payment -> paymentService.createForDecision(errandId, toPaymentRequest(payment)))
			.toList();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Process
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Resume the process waiting at the decision gateway: {@code paymentDecision=APPROVED} for a granting outcome (the
	 * process commits the normberäkning, sets GRANTED and polls the payment), {@code REJECTED} otherwise. The status is
	 * the process's to set, never this service's. Best-effort: an unreachable engine is reported as
	 * {@code processMessageCorrelated=false}, and the message is queued for the scheduled retry in the transaction that
	 * records the decision — so a saved decision cannot leave its process waiting before the decision gateway. If the
	 * queueing itself fails, so does finalize, and nothing is saved.
	 */
	private boolean correlatePaymentDecision(final String municipalityId, final String namespace, final String errandId, final String outcome) {
		final String paymentDecision;
		if (outcomeCarriesAmount(outcome)) {
			paymentDecision = PAYMENT_DECISION_APPROVED;
		} else {
			paymentDecision = PAYMENT_DECISION_REJECTED;
		}
		final Map<String, Object> variables = Map.of(VARIABLE_PAYMENT_DECISION, paymentDecision);
		try {
			processService.correlateMessage(municipalityId, namespace, MESSAGE_PAYMENT_DECISION_RECEIVED, errandId, variables);
			return true;
		} catch (final Exception e) {
			LOG.warn("Could not correlate {} for errand {} — decision recorded, message queued for retry", sanitizeForLogging(MESSAGE_PAYMENT_DECISION_RECEIVED), sanitizeForLogging(errandId), e);
			processService.queueMessageRetry(municipalityId, namespace, MESSAGE_PAYMENT_DECISION_RECEIVED, errandId, variables, e.getMessage());
			return false;
		}
	}
}
