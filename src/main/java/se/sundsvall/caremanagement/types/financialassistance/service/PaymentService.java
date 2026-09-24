package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

/**
 * Read-only access to the payment rows errands carry from the time careM held payment drafts and "Besluta och
 * utbetala" created one row per decided payment. That path is retired: Draken registers payments directly in Lifecare,
 * finalize creates no rows, and the payments API is gone. The rows are kept, untouched, because errands decided before
 * the change — and the process instances still polling them — are verified against them by payment-status, and because
 * they are audit history. Nothing writes to them any more.
 */
@Service
public class PaymentService {

	static final String SOURCE_CASEWORKER = "CASEWORKER";
	/** A row the caseworker saved but that no decision took — never a decided payment. */
	static final String STATUS_DRAFT = "DRAFT";
	/** A decided row that Draken's BFF reported registered in Lifecare, with its Lifecare id. */
	static final String STATUS_REGISTERED = "REGISTERED";

	private final ErrandService errandService;
	private final FaPaymentRepository paymentRepository;

	PaymentService(final ErrandService errandService, final FaPaymentRepository paymentRepository) {
		this.errandService = errandService;
		this.paymentRepository = paymentRepository;
	}

	/** The payment rows on an errand, oldest first. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public List<Payment> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return paymentRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaPaymentEntity::getCreated, nullsLast(naturalOrder())))
			.map(PaymentService::toPayment)
			.toList();
	}

	/**
	 * Which of the given Lifecare payment ids another errand's payment rows already carry. Not scoped — an internal
	 * question for payment-status, which must never take another errand's payment as this one's.
	 */
	@Transactional(readOnly = true)
	public List<String> lifecareIdsOnOtherErrands(final Collection<String> lifecareIds, final String errandId) {
		if (lifecareIds.isEmpty()) {
			return List.of();
		}
		return paymentRepository.findLifecareIdsOnOtherErrands(lifecareIds, errandId);
	}

	/** The fields payment-status reads: provenance, status, the Lifecare id and the dates. */
	private static Payment toPayment(final FaPaymentEntity entity) {
		return Payment.create()
			.withId(entity.getId())
			.withSource(entity.getSource())
			.withLifecareId(entity.getLifecareId())
			.withStatus(entity.getStatus())
			.withPaymentDate(entity.getPaymentDate())
			.withAmount(entity.getAmount())
			.withApplicationMonth(entity.getApplicationMonth())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}
}
