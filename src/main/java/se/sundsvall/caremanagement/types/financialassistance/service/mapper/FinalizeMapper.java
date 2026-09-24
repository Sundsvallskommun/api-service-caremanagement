package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.decisions.service.DecisionService.LIFECARE_STATUS_PENDING;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;

/**
 * Mappings for the finalize ("Besluta och utbetala") step: the request → the {@code PAYMENT} decision row, the request
 * → the entity's audit fields, and the request's payments → {@link PaymentRequest}s.
 */
public final class FinalizeMapper {

	/** The decision type of the caseworker's payment decision — the audit-trail row finalize records. */
	public static final String DECISION_TYPE_PAYMENT = "PAYMENT";

	private FinalizeMapper() {}

	/**
	 * The {@code PAYMENT} decision the finalize records on the errand. The outcome is the value, the internal reason the
	 * description, the underrättelse the decision message. A non-granting outcome is recorded with amount 0 whatever the
	 * request carried, matching the decision model's "0 for a rejection".
	 */
	public static Decision toPaymentDecision(final FinalizeRequest request, final String decidedBy, final LocalDate decisionDate) {
		return ofNullable(request)
			.map(FinalizeRequest::getDecision)
			.map(decision -> Decision.create()
				.withDecisionType(DECISION_TYPE_PAYMENT)
				.withValue(decision.getOutcome())
				.withDescription(decision.getReason())
				.withCoApplicantReason(decision.getCoApplicantReason())
				.withAmount(effectiveAmount(decision))
				.withDecisionMessage(decision.getDecisionMessage())
				.withDecisionDate(decisionDate)
				.withPeriodFrom(decision.getPeriodFrom())
				.withPeriodTo(decision.getPeriodTo())
				.withCreatedBy(decidedBy)
				// Written into Lifecare by Draken's BFF; its lifecare-result report moves it to SYNCED or FAILED.
				.withLifecareStatus(LIFECARE_STATUS_PENDING))
			.orElse(null);
	}

	/** The amount the decision grants: the request's for a granting outcome, 0 for avslag/avvisning. */
	static BigDecimal effectiveAmount(final FinalizeDecision decision) {
		if (outcomeCarriesAmount(decision.getOutcome())) {
			return ofNullable(decision.getAmount()).orElse(BigDecimal.ZERO);
		}
		return BigDecimal.ZERO;
	}

	/**
	 * Stamp the finalize choices on the errand: the communication channels and the household-size flag (audit trail,
	 * surfaced on the view). A missing flag reads as false.
	 */
	public static FinancialAssistanceEntity updateEntity(final FinancialAssistanceEntity entity, final FinalizeRequest request) {
		return ofNullable(entity)
			.map(target -> {
				final var channels = ofNullable(request).map(FinalizeRequest::getCommunication).orElseGet(CommunicationChannels::create);
				return target
					.withHouseholdSizeChanged(ofNullable(request).map(FinalizeRequest::getHouseholdSizeChanged).orElse(false))
					.withNotifyMinaSidor(ofNullable(channels.getMinaSidor()).orElse(false))
					.withNotifyDigitalMailbox(ofNullable(channels.getDigitalMailbox()).orElse(false))
					.withNotifyLetter(ofNullable(channels.getLetter()).orElse(false));
			})
			.orElse(null);
	}

	/** The finalize payment as a {@link PaymentRequest}, bridging the two models' naming. */
	public static PaymentRequest toPaymentRequest(final FinalizePayment payment) {
		final var request = PaymentRequest.create();
		ofNullable(payment).ifPresent(source -> {
			request.setPaymentDate(source.getPaymentDate());
			request.setAmount(source.getAmount());
			// concernedMonth on the finalize model, applicationMonth on the payment - same yyyy-MM, different word
			request.setApplicationMonth(source.getConcernedMonth());
			request.setAccountingCode(source.getAccountingCode());
			request.setLocalPaymentNumber(source.getLocalPaymentNumber());
			request.setInvoiceNumber(source.getInvoiceNumber());
			ofNullable(source.getPayee()).ifPresent(payee -> {
				// The payee row's id, when the caseworker picked one from the errand's payee list, and the payee's
				// Lifecare id, when they picked one Lifecare already has. Either carries the link to lifecarePayeeId
				// through to GET .../payments/{paymentId}, so the payment can be registered against the payee in Lifecare
				// by id instead of by matching on name and account number.
				request.setPayeeId(payee.getId());
				request.setLifecarePayeeId(payee.getLifecarePayeeId());
				request.setPayeeName(payee.getName());
				request.setPaymentMethod(payee.getPaymentMethod());
				request.setClearingNumber(payee.getClearing());
				request.setAccountNumber(payee.getAccountNumber());
				request.setPayeeAddress(payee.getAddress());
				request.setPayeeCareOf(payee.getCareOf());
				request.setPayeeZipCode(payee.getZipCode());
				request.setPayeeCity(payee.getCity());
			});
		});
		return request;
	}
}
