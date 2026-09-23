package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentLifecareResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPayeeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPayeeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;

/**
 * financial assistance payments (utbetalningar) — caseworker-drafted or Lifecare-mirrored payment rows on an errand.
 * Every method is scoped to the errand's namespace/municipality (404 when the errand is missing there). Unlike
 * {@link MonitoringService monitorings}, several payments per {@code applicationMonth} are allowed — there is no
 * uniqueness check on that field, only on {@code (errand, lifecareId)}.
 *
 * <p>
 * A payment carries a {@code source} ({@code CASEWORKER}, the default, or {@code LIFECARE}) and an optional
 * {@code lifecareId}, exactly like {@link MonitoringService monitorings}: RPA surfaces a Lifecare payment by POSTing
 * {@code source=LIFECARE} with its {@code lifecareId} (idempotent per errand + lifecareId, so re-runs don't
 * duplicate), and mirrors a caseworker payment the other way, later stamping back the {@code lifecareId} it was given
 * in Lifecare. The provenance fields are system/RPA-managed: an update only touches them when supplied, so a
 * caseworker edit never drops them.
 * </p>
 *
 * <p>
 * {@code status} is entirely server-managed and is deliberately absent from {@link PaymentRequest} — a create always
 * yields {@code DRAFT}, and this method does <em>not</em> enqueue any RPA task. A caseworker must be able to save a
 * draft payment without setting the robot off; queuing the {@code REGISTER_PAYMENT} RPA task (which fetches the rest
 * of the payment via {@code GET .../payments/{paymentId}}, carrying only the {@code paymentId} in the queue item, so
 * personal data never enters the Orchestrator queue) is a separate, explicit {@code POST .../rpa-tasks} call — the
 * same two-call shape every other RPA action in this service uses. The one thing that does move {@code status} is the
 * robot's own report through {@link #recordLifecareResult}: {@code PENDING_REGISTRATION} becomes {@code REGISTERED}
 * or {@code FAILED} when the {@code REGISTER_PAYMENT} robot says what happened.
 * </p>
 */
@Service
public class PaymentService {

	static final String SOURCE_CASEWORKER = "CASEWORKER";
	static final String SOURCE_LIFECARE = "LIFECARE";
	static final String STATUS_DRAFT = "DRAFT";
	/**
	 * A payment created by "Besluta och utbetala" and queued to the robot. Deliberately not {@code DRAFT}: a row the
	 * caseworker has decided on is not a draft, and calling it one would make the status useless for seeing what is
	 * actually waiting to be registered in Lifecare. The {@code REGISTER_PAYMENT} robot clears it by reporting through
	 * {@link #recordLifecareResult}.
	 */
	static final String STATUS_PENDING_REGISTRATION = "PENDING_REGISTRATION";

	/**
	 * The robot reported the payment into Lifecare. It exists there; whether it has been paid out is a separate question.
	 */
	static final String STATUS_REGISTERED = "REGISTERED";

	/** The robot could not register the payment. Lifecare's own reason is on the row, and the caseworker sees it. */
	static final String STATUS_FAILED = "FAILED";

	static final String OUTCOME_FAILED = "FAILED";

	private static final String ERROR_DETAIL_REQUIRED = "detail is required when outcome is FAILED - it must be Lifecare's own message, since it is shown to the caseworker";
	private static final String ERROR_ALREADY_REGISTERED = "payment is already REGISTERED in Lifecare - a later FAILED would silently un-register a payment that exists there";

	private final ErrandService errandService;
	private final FaPaymentRepository paymentRepository;
	private final FaPayeeRepository payeeRepository;

	PaymentService(final ErrandService errandService, final FaPaymentRepository paymentRepository, final FaPayeeRepository payeeRepository) {
		this.errandService = errandService;
		this.paymentRepository = paymentRepository;
		this.payeeRepository = payeeRepository;
	}

	/** The payments on an errand, oldest first. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public List<Payment> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return paymentRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaPaymentEntity::getCreated, nullsLast(naturalOrder())))
			.map(this::toPayment)
			.toList();
	}

	/** How many payments are on an errand. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public long count(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return paymentRepository.countByErrandId(errandId);
	}

	/** A single payment on an errand. Scoped: throws {@code 404} when the errand or payment is missing here. */
	@Transactional(readOnly = true)
	public Payment get(final String municipalityId, final String namespace, final String errandId, final String paymentId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return toPayment(requirePayment(errandId, paymentId));
	}

	/**
	 * Create a payment on an errand as a {@code DRAFT} — or, for a Lifecare-sourced one carrying a {@code lifecareId},
	 * upsert it so RPA re-runs don't duplicate. Does not enqueue any RPA task; see this class's javadoc. Scoped: throws
	 * {@code 404} when the errand is missing.
	 */
	@Transactional
	public Payment create(final String municipalityId, final String namespace, final String errandId, final PaymentRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		final FaPaymentEntity entity;
		if (hasText(request.getLifecareId())) {
			entity = paymentRepository.findByErrandIdAndLifecareId(errandId, request.getLifecareId()).orElseGet(FaPaymentEntity::create);
		} else {
			entity = FaPaymentEntity.create();
		}

		return toPayment(paymentRepository.save(applyRequest(entity, request)
			.withErrandId(errandId)
			.withSource(resolveSource(request.getSource()))
			.withLifecareId(request.getLifecareId())
			.withStatus(STATUS_DRAFT)));
	}

	/**
	 * Create the payment a finalize ("Besluta och utbetala") decided on, as {@code PENDING_REGISTRATION}, and return
	 * its id for the {@code REGISTER_PAYMENT} queue item. Separate from {@link #create} because that one is the
	 * caseworker's draft-saving path and must keep yielding {@code DRAFT}.
	 *
	 * <p>
	 * The errand is <em>not</em> scope-checked here: finalize has already read it, and this runs inside finalize's
	 * transaction, so a failure has to roll the decision back with it rather than leave an errand with a decision and
	 * no payments.
	 * </p>
	 */
	@Transactional
	public String createForDecision(final String errandId, final PaymentRequest request) {
		// saveAndFlush, not save: an ordinary save only queues the insert in the persistence context, and JPA flushes
		// it at commit - after finalize has already correlated the process message and queued the RPA items, which are
		// external and cannot be rolled back. A constraint violation would then leave the engine believing a decision
		// was made while careM has no decision and no payment row. Flushing here makes the database the first thing
		// that can fail, so the rollback is complete.
		return paymentRepository.saveAndFlush(applyRequest(FaPaymentEntity.create(), request)
			.withErrandId(errandId)
			.withSource(SOURCE_CASEWORKER)
			.withStatus(STATUS_PENDING_REGISTRATION)).getId();
	}

	/**
	 * Replace a payment's mutable fields. {@code status} is never touched here — it is server-managed. Scoped: throws
	 * {@code 404} when the errand or payment is missing.
	 */
	@Transactional
	public Payment update(final String municipalityId, final String namespace, final String errandId, final String paymentId, final PaymentRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var entity = applyRequest(requirePayment(errandId, paymentId), request);
		// Provenance is system/RPA-managed: only overwrite when the request supplies it, so a caseworker edit keeps it.
		if (hasText(request.getSource())) {
			entity.setSource(resolveSource(request.getSource()));
		}
		if (hasText(request.getLifecareId())) {
			entity.setLifecareId(request.getLifecareId());
		}
		return toPayment(paymentRepository.save(entity));
	}

	/**
	 * Record the {@code REGISTER_PAYMENT} robot's report — the counterpart of the {@code ADD_PAYEE} one, and the only
	 * thing that moves a payment out of {@code PENDING_REGISTRATION}. {@code REGISTERED} and {@code ALREADY_EXISTS}
	 * both count as success: the caseworker's intent, "this payment must exist in Lifecare", is satisfied either way.
	 *
	 * <p>
	 * The Lifecare id goes into the existing {@code lifecareId} — the field was always documented as the payment's id
	 * in Lifecare once it exists there. Re-posting the same outcome is idempotent; reporting {@code FAILED} on a row
	 * already {@code REGISTERED} is a {@code 409}, because that would silently un-register a payment that exists in
	 * Lifecare, exactly as it would for a payee.
	 * </p>
	 */
	@Transactional
	public Payment recordLifecareResult(final String municipalityId, final String namespace, final String errandId, final String paymentId,
		final PaymentLifecareResult result) {

		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var entity = requirePayment(errandId, paymentId);

		if (OUTCOME_FAILED.equals(result.getOutcome())) {
			if (!hasText(result.getDetail())) {
				throw Problem.valueOf(BAD_REQUEST, ERROR_DETAIL_REQUIRED);
			}
			if (STATUS_REGISTERED.equals(entity.getStatus())) {
				throw Problem.valueOf(CONFLICT, ERROR_ALREADY_REGISTERED);
			}
			return toPayment(paymentRepository.save(entity
				.withStatus(STATUS_FAILED)
				.withLifecareDetail(result.getDetail())));
		}

		// A re-report without an id must not wipe the one an earlier report already stored.
		final var lifecareId = ofNullable(result.getLifecarePaymentId()).filter(StringUtils::hasText).orElse(entity.getLifecareId());
		return toPayment(paymentRepository.save(entity
			.withStatus(STATUS_REGISTERED)
			.withLifecareId(lifecareId)
			.withLifecareDetail(null)));
	}

	/** Remove a payment from an errand. Scoped: throws {@code 404} when the errand or payment is missing here. */
	@Transactional
	public void delete(final String municipalityId, final String namespace, final String errandId, final String paymentId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		paymentRepository.delete(requirePayment(errandId, paymentId));
	}

	private FaPaymentEntity requirePayment(final String errandId, final String paymentId) {
		return paymentRepository.findByIdAndErrandId(paymentId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Payment not found on errand"));
	}

	/** The supplied source, defaulting to {@link #SOURCE_CASEWORKER} when blank (the form omits it for caseworker rows). */
	private static String resolveSource(final String source) {
		if (hasText(source)) {
			return source;
		}
		return SOURCE_CASEWORKER;
	}

	private static FaPaymentEntity applyRequest(final FaPaymentEntity entity, final PaymentRequest request) {
		return entity
			.withMoneyType(request.getMoneyType())
			.withPaymentDate(request.getPaymentDate())
			.withAmount(request.getAmount())
			.withApplicationMonth(request.getApplicationMonth())
			.withAccountingCode(request.getAccountingCode())
			.withReportedOnStakeholderIds(request.getReportedOnStakeholderIds())
			.withAccountingDate(request.getAccountingDate())
			.withExcludedFromPayment(request.isExcludedFromPayment())
			.withPayeeId(request.getPayeeId())
			.withLifecarePayeeId(request.getLifecarePayeeId())
			.withPayeeStakeholderId(request.getPayeeStakeholderId())
			.withPaymentMethod(request.getPaymentMethod())
			.withPayeeName(request.getPayeeName())
			.withPayeeAddress(request.getPayeeAddress())
			.withPayeeCareOf(request.getPayeeCareOf())
			.withPayeeZipCode(request.getPayeeZipCode())
			.withPayeeCity(request.getPayeeCity())
			.withClearingNumber(request.getClearingNumber())
			.withAccountNumber(request.getAccountNumber())
			.withLocalPaymentNumber(request.getLocalPaymentNumber())
			.withInvoiceNumber(request.getInvoiceNumber())
			.withUsesOcr(request.isUsesOcr())
			.withMessageLines(request.getMessageLines());
	}

	/**
	 * The payment as the API serves it. {@code lifecarePayeeId} is the one given with the payment when the caseworker
	 * picked a payee straight from Lifecare. Otherwise it is read from the payee row rather than copied at create time:
	 * the ADD_PAYEE robot reports it back onto that row, possibly after this payment was created, so a copy would go
	 * stale. Null when neither is there.
	 */
	private Payment toPayment(final FaPaymentEntity entity) {
		return Payment.create()
			.withId(entity.getId())
			.withSource(entity.getSource())
			.withLifecareId(entity.getLifecareId())
			.withLifecareDetail(entity.getLifecareDetail())
			.withStatus(entity.getStatus())
			.withMoneyType(entity.getMoneyType())
			.withPaymentDate(entity.getPaymentDate())
			.withAmount(entity.getAmount())
			.withApplicationMonth(entity.getApplicationMonth())
			.withAccountingCode(entity.getAccountingCode())
			.withReportedOnStakeholderIds(entity.getReportedOnStakeholderIds())
			.withAccountingDate(entity.getAccountingDate())
			.withExcludedFromPayment(entity.isExcludedFromPayment())
			.withPayeeId(entity.getPayeeId())
			.withLifecarePayeeId(lifecarePayeeId(entity))
			.withPayeeStakeholderId(entity.getPayeeStakeholderId())
			.withPaymentMethod(entity.getPaymentMethod())
			.withPayeeName(entity.getPayeeName())
			.withPayeeAddress(entity.getPayeeAddress())
			.withPayeeCareOf(entity.getPayeeCareOf())
			.withPayeeZipCode(entity.getPayeeZipCode())
			.withPayeeCity(entity.getPayeeCity())
			.withClearingNumber(entity.getClearingNumber())
			.withAccountNumber(entity.getAccountNumber())
			.withLocalPaymentNumber(entity.getLocalPaymentNumber())
			.withInvoiceNumber(entity.getInvoiceNumber())
			.withUsesOcr(entity.isUsesOcr())
			.withMessageLines(entity.getMessageLines())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}

	/** The Lifecare id given with the payment, else the payee row's, else null (no row, or a deleted one). */
	private String lifecarePayeeId(final FaPaymentEntity entity) {
		if (hasText(entity.getLifecarePayeeId())) {
			return entity.getLifecarePayeeId();
		}
		final var payeeId = entity.getPayeeId();
		if (!hasText(payeeId)) {
			return null;
		}
		return payeeRepository.findById(payeeId)
			.map(FaPayeeEntity::getLifecarePayeeId)
			.orElse(null);
	}
}
