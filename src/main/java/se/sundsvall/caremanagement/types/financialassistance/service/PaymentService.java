package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
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
 * same two-call shape every other RPA action in this service uses. Nothing else in this service ever changes
 * {@code status}; a later worker/callback updates it out of band as the robot processes the queued task.
 * </p>
 */
@Service
public class PaymentService {

	static final String SOURCE_CASEWORKER = "CASEWORKER";
	static final String SOURCE_LIFECARE = "LIFECARE";
	static final String STATUS_DRAFT = "DRAFT";

	private final ErrandService errandService;
	private final FaPaymentRepository paymentRepository;

	PaymentService(final ErrandService errandService, final FaPaymentRepository paymentRepository) {
		this.errandService = errandService;
		this.paymentRepository = paymentRepository;
	}

	/** The payments on an errand, oldest first. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public List<Payment> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return paymentRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaPaymentEntity::getCreated, nullsLast(naturalOrder())))
			.map(PaymentService::toPayment)
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
			.withReportedOnStakeholderIds(request.getReportedOnStakeholderIds())
			.withAccountingDate(request.getAccountingDate())
			.withExcludedFromPayment(request.isExcludedFromPayment())
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

	private static Payment toPayment(final FaPaymentEntity entity) {
		return Payment.create()
			.withId(entity.getId())
			.withSource(entity.getSource())
			.withLifecareId(entity.getLifecareId())
			.withStatus(entity.getStatus())
			.withMoneyType(entity.getMoneyType())
			.withPaymentDate(entity.getPaymentDate())
			.withAmount(entity.getAmount())
			.withApplicationMonth(entity.getApplicationMonth())
			.withReportedOnStakeholderIds(entity.getReportedOnStakeholderIds())
			.withAccountingDate(entity.getAccountingDate())
			.withExcludedFromPayment(entity.isExcludedFromPayment())
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
}
