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
import se.sundsvall.caremanagement.decisions.api.model.DecisionLifecareResult;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionRegistration;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaCalculationDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
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
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.updateEntity;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Besluta och utbetala — the caseworker's decision step on a financial assistance (återansökan) errand. Once the
 * errand waits for a decision and the Lifecare artefacts the decision rests on are linked to it, one call does, in
 * order:
 *
 * <ol>
 * <li>records the finalize choices on the errand (communication channels, household-size flag);</li>
 * <li>records the decision as a {@code PAYMENT} {@code Decision} row — the audit trail — and receipts it against the
 * errand's beslut in Lifecare ({@code lifecareDecisionId}): the row is marked {@code SYNCED} with Lifecare's id, as a
 * {@code WRITTEN} report to {@code .../decisions/{decisionId}/lifecare-result} would. The beslut is already in Lifecare
 * (Draken saves it there before finalizing), so this is only the link between the two, made in the same transaction.
 * A client that still posts that report afterwards changes nothing: re-posting WRITTEN is idempotent;</li>
 * <li>correlates {@code PaymentDecisionReceived} to the waiting process, which then sets the status and, for a bifall,
 * checks the linked Lifecare payments until they are paid out;</li>
 * <li>purges careM's normberäkning draft when the errand is linked to a normberäkning saved in Lifecare
 * ({@code lifecareCalculationId}): the draft was only a proposal, frozen since that id was set, and the decided
 * calculation is Lifecare's. Keeping it would store the household's incomes and expenses twice. Without the id (an
 * avslag decided without a saved calculation) the draft is the only trace of the proposal and stays for the errand's
 * own
 * disposal.</li>
 * </ol>
 *
 * <p>
 * The preconditions are the real ones, not caseworker check-offs: Draken saves the beslut in Lifecare and links it as
 * {@code lifecareDecisionId} (every outcome); for a granting outcome (BIFALL/DELAVSLAG) it also saves the normberäkning
 * and links it as {@code lifecareCalculationId}, both through {@code PATCH .../data} before this call, and registers
 * the
 * payments directly in Lifecare. finalize refuses an errand that lacks a required reference, and an avslag linked to
 * payments. These are references: careM does not claim that the calculation is final, the beslut locked or a
 * payment paid out because an id is there — those statuses are Lifecare's, read from Lifecare (payment-status for the
 * process; Draken reads the rest itself). careM creates no payments, and no payee state affects this call.
 *
 * <p>
 * Step 3 is best-effort and reported in the response rather than failing the call: the decision is recorded either
 * way, and an uncorrelated message is queued, in this transaction, for the scheduled process-message retry. The
 * Lifecare writes — decision, utbetalningar, bevakningar, journal, documents — are all the Draken BFF's, done directly
 * against Lifecare. Sending the decision to the applicant (step 6 of the verksamhet's flow) is the BFF's job too; this
 * service only records and echoes the chosen channels.
 */
@Service
@Transactional
public class FinancialAssistanceFinalizeService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceFinalizeService.class);

	private static final String ERROR_NO_TYPED_ERRAND = "No financial-assistance errand for id %s";
	private static final String ERROR_NO_DECIDER = "a decision can only be recorded by an identified user - the X-Sent-By header is required";
	private static final String ERROR_NO_DECISION = "the finalize request carries no decision";
	private static final String LIFECARE_RESULT_WRITTEN = "WRITTEN";
	private static final String REGISTRATION_REGISTERED = "REGISTERED";
	private static final String ERROR_WRONG_STATUS = "errand must be in status %s to be finalized, but is in status '%s'";
	private static final String ERROR_ALREADY_FINALIZED = "errand '%s' already carries a %s decision - it has been finalized";
	private static final String ERROR_NO_LIFECARE_DECISION = "a decision requires the beslut to be saved in Lifecare first - save it and set lifecareDecisionId on errand '%s' (PATCH .../financial-assistance/{errandId}/data) before finalizing";
	private static final String ERROR_NO_LIFECARE_CALCULATION = "a %s decision requires the normberäkning to be saved in Lifecare first - save it and set lifecareCalculationId on errand '%s' (PATCH .../financial-assistance/{errandId}/data) before finalizing";
	private static final String ERROR_PAYMENTS_ON_REJECTION = "an %s decision pays nothing, but errand '%s' is linked to Lifecare payments %s - remove them in Lifecare and clear lifecarePaymentIds before finalizing";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final DecisionService decisionService;
	private final ProcessService processService;
	private final FaCalculationDraftRepository calculationDraftRepository;

	FinancialAssistanceFinalizeService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository,
		final DecisionService decisionService, final ProcessService processService, final FaCalculationDraftRepository calculationDraftRepository) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.decisionService = decisionService;
		this.processService = processService;
		this.calculationDraftRepository = calculationDraftRepository;
	}

	/**
	 * Finalize the errand — see the class description for the steps. Scoped: {@code 404} when the errand is missing in
	 * this namespace/municipality; {@code 400} without an identified caller; {@code 409} when the errand is not
	 * {@code AWAITING_DECISION}, when a {@code PAYMENT} decision already exists, when the errand lacks a Lifecare
	 * reference the outcome needs, or when an avslag is linked to payments.
	 *
	 * @param  decidedBy the authenticated caseworker (X-Sent-By) — becomes the decision's {@code createdBy}
	 * @return           the receipt: decision id, whether the process was resumed, the channels
	 */
	public FinalizeResponse finalize(final String municipalityId, final String namespace, final String errandId, final FinalizeRequest request, final String decidedBy) {
		final var errand = errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		requireDecider(decidedBy);
		requireDecision(request);
		requireStatus(errand.getStatus());
		requireNotFinalized(municipalityId, namespace, errandId);
		final var entity = financialAssistanceRepository.findByErrandId(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_NO_TYPED_ERRAND.formatted(errandId)));
		final var outcome = request.getDecision().getOutcome();
		requireLifecareReferences(entity, errandId, outcome);

		// 1. The audit fields first (communication channels, household-size flag), so they are on the errand before the
		// process is resumed in step 3.
		financialAssistanceRepository.save(updateEntity(entity, request));

		// 2. The decision row, receipted against the beslut already in Lifecare.
		final var decisionId = decisionService.create(municipalityId, namespace, errandId,
			toPaymentDecision(request, decidedBy, LocalDate.now(ZoneId.systemDefault())));
		final var lifecareId = String.valueOf(entity.getLifecareDecisionId());
		decisionService.recordLifecareResult(municipalityId, namespace, errandId, decisionId,
			DecisionLifecareResult.create().withOutcome(LIFECARE_RESULT_WRITTEN).withLifecareId(lifecareId));

		// 3. Resume the process.
		final var correlated = correlatePaymentDecision(municipalityId, namespace, errandId, outcome);

		// 4. The decided normberäkning is Lifecare's; careM's frozen proposal is disposed of.
		purgeCalculationDraft(entity, errandId);

		LOG.info("Finalized errand {} with outcome {} (decision {}, process correlated: {})", sanitizeForLogging(errandId),
			sanitizeForLogging(outcome), sanitizeForLogging(decisionId), correlated);

		return FinalizeResponse.create()
			.withDecisionId(decisionId)
			.withProcessMessageCorrelated(correlated)
			.withCommunication(request.getCommunication())
			.withLifecareDecision(new LifecareDecisionRegistration(decisionId, REGISTRATION_REGISTERED, lifecareId, null));
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
	 * A request without a decision is completed from Lifecare before it gets here; one that is not has nothing to record.
	 */
	private static void requireDecision(final FinalizeRequest request) {
		if (request == null || request.getDecision() == null) {
			throw Problem.valueOf(BAD_REQUEST, ERROR_NO_DECISION);
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

	/** A second finalize would record a second decision and re-correlate a process that already moved on. */
	private void requireNotFinalized(final String municipalityId, final String namespace, final String errandId) {
		final var alreadyDecided = decisionService.readAll(municipalityId, namespace, errandId).stream()
			.anyMatch(decision -> DECISION_TYPE_PAYMENT.equals(decision.getDecisionType()));
		if (alreadyDecided) {
			throw Problem.valueOf(CONFLICT, ERROR_ALREADY_FINALIZED.formatted(errandId, DECISION_TYPE_PAYMENT));
		}
	}

	/**
	 * The Lifecare artefacts the decision rests on must be linked to the errand. Every outcome is a beslut Draken saves in
	 * Lifecare first, so {@code lifecareDecisionId} is always required. A granting outcome (BIFALL/DELAVSLAG) is paid
	 * against the normberäkning in Lifecare, so it also needs {@code lifecareCalculationId}. Its payments are not a
	 * precondition here: Draken registers them in Lifecare from its payment form without handing careM their ids, and the
	 * process does not move the errand to PAID until payment-status has found them paid in Lifecare, on the errand's own
	 * insats — a bifall whose payment is missing waits and is escalated to the caseworker, it never closes. An avslag
	 * pays nothing: it needs no calculation, and a linked payment means money was registered in Lifecare for a decision
	 * that grants none, so it is refused rather than recorded.
	 */
	private static void requireLifecareReferences(final FinancialAssistanceEntity entity, final String errandId, final String outcome) {
		if (entity.getLifecareDecisionId() == null) {
			throw Problem.valueOf(CONFLICT, ERROR_NO_LIFECARE_DECISION.formatted(errandId));
		}
		final var lifecarePaymentIds = linkedPaymentIds(entity);
		if (!outcomeCarriesAmount(outcome)) {
			if (!lifecarePaymentIds.isEmpty()) {
				throw Problem.valueOf(CONFLICT, ERROR_PAYMENTS_ON_REJECTION.formatted(outcome, errandId, lifecarePaymentIds));
			}
			return;
		}
		if (entity.getLifecareCalculationId() == null) {
			throw Problem.valueOf(CONFLICT, ERROR_NO_LIFECARE_CALCULATION.formatted(outcome, errandId));
		}
	}

	private static List<String> linkedPaymentIds(final FinancialAssistanceEntity entity) {
		return ofNullable(entity.getLifecarePaymentIds()).orElseGet(List::of);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Disposal
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Delete careM's normberäkning draft once the decision rests on a normberäkning saved in Lifecare. The row cascades to
	 * the draft's norm types, persons, incomes and expenses. The daily prepare never rebuilds it: it leaves the draft
	 * alone while {@code lifecareCalculationId} is set, and a decided errand is not prepared again.
	 */
	private void purgeCalculationDraft(final FinancialAssistanceEntity entity, final String errandId) {
		if (entity.getLifecareCalculationId() == null || !calculationDraftRepository.existsById(errandId)) {
			return;
		}
		calculationDraftRepository.deleteById(errandId);
		LOG.info("Purged the normberäkning draft of errand {}: the decided calculation is Lifecare's", sanitizeForLogging(errandId));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Process
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Resume the process waiting at the decision gateway: {@code paymentDecision=APPROVED} for a granting outcome (the
	 * process sets GRANTED and checks the payments in Lifecare), {@code REJECTED} otherwise. The status is the process's to
	 * set, never this service's. Best-effort: an unreachable engine is reported as {@code processMessageCorrelated=false},
	 * and the message is queued for the scheduled retry in the transaction that records the decision — so a saved
	 * decision cannot leave its process waiting before the decision gateway. If the queueing itself fails, so does
	 * finalize, and nothing is saved.
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
