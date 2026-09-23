package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.rpa.service.RpaAction;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RpaTask;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.decisions.service.DecisionService.LIFECARE_STATUS_PENDING;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;

/**
 * Mappings for the finalize ("Besluta och utbetala") step: the request → the {@code PAYMENT} decision row, the
 * request → the entity's audit fields, and the request parts → the flat string maps the UiPath queue items carry.
 *
 * <p>
 * The queue-item maps are deliberately compact: values Lifecare needs typed in, plus ids the robot fetches details for
 * through the existing GET endpoints. They never carry personal numbers — the robot gets those from the errand's
 * {@code rpa-context} endpoint.
 */
public final class FinalizeMapper {

	/** The decision type of the caseworker's payment decision — the audit-trail row finalize records. */
	public static final String DECISION_TYPE_PAYMENT = "PAYMENT";

	static final String CHANNEL_MINA_SIDOR = "MINA_SIDOR";
	static final String CHANNEL_DIGITAL_MAILBOX = "DIGITAL_MAILBOX";
	static final String CHANNEL_LETTER = "LETTER";

	static final String KEY_DECISION_ID = "decisionId";
	static final String KEY_OUTCOME = "outcome";
	static final String KEY_REASON = "reason";
	static final String KEY_PERIOD_FROM = "periodFrom";
	static final String KEY_PERIOD_TO = "periodTo";
	static final String KEY_AMOUNT = "amount";
	static final String KEY_COMMUNICATION_CHANNELS = "communicationChannels";
	static final String KEY_HOUSEHOLD_SIZE_CHANGED = "householdSizeChanged";
	/** The only key a REGISTER_PAYMENT item needs: the robot reads the payment itself through the Payment resource. */
	static final String KEY_PAYMENT_ID = "paymentId";

	// The pre-2026-09-21 REGISTER_PAYMENT keys, kept for a robot that has not been released against the paymentId
	// contract yet. Transitional: see toLegacyPaymentContent.
	static final String KEY_SEQUENCE = "sequence";
	static final String KEY_PAYMENT_DATE = "paymentDate";
	static final String KEY_CONCERNED_MONTH = "concernedMonth";
	static final String KEY_ACCOUNTING_CODE = "accountingCode";
	static final String KEY_PAYEE_NAME = "payeeName";
	static final String KEY_PAYMENT_METHOD = "paymentMethod";
	static final String KEY_CLEARING = "clearing";
	static final String KEY_ACCOUNT_NUMBER = "accountNumber";
	static final String KEY_COUNT = "count";

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
				.withAmount(effectiveAmount(decision))
				.withDecisionMessage(decision.getDecisionMessage())
				.withDecisionDate(decisionDate)
				.withPeriodFrom(decision.getPeriodFrom())
				.withPeriodTo(decision.getPeriodTo())
				.withCreatedBy(decidedBy)
				// Handed over to be written into Lifecare; the writer's report moves it to SYNCED or FAILED.
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
	 * Stamp the finalize choices on the errand: the communication channels (audit trail, surfaced on the view) and the
	 * household-size flag the {@code WRITE_NORMBERAKNING} item forwards to the robot. A missing flag reads as false.
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

	/** The {@code WRITE_DECISION} queue item content — the decision as typed into Lifecare, plus the decision row's id. */
	public static Map<String, String> toDecisionContent(final FinalizeRequest request, final String decisionId) {
		final var content = new HashMap<String, String>();
		put(content, KEY_DECISION_ID, decisionId);
		ofNullable(request).map(FinalizeRequest::getDecision).ifPresent(decision -> {
			put(content, KEY_OUTCOME, decision.getOutcome());
			put(content, KEY_REASON, decision.getReason());
			put(content, KEY_PERIOD_FROM, decision.getPeriodFrom());
			put(content, KEY_PERIOD_TO, decision.getPeriodTo());
			put(content, KEY_AMOUNT, effectiveAmount(decision));
		});
		ofNullable(request).ifPresent(source -> {
			put(content, KEY_COMMUNICATION_CHANNELS, toChannelList(source.getCommunication()));
			put(content, KEY_HOUSEHOLD_SIZE_CHANGED, ofNullable(source.getHouseholdSizeChanged()).orElse(false));
		});
		return content;
	}

	/**
	 * One {@code REGISTER_PAYMENT} queue item content: the {@code paymentId} and nothing else. The robot fetches the
	 * rest via {@code GET .../payments/{paymentId}}, which is what keeps the payee's name, clearing and account number
	 * out of the Orchestrator queue store — the same reason personal numbers are served through {@code rpa-context}
	 * rather than carried on the item.
	 */
	public static Map<String, String> toPaymentIdContent(final String paymentId) {
		final var content = new HashMap<String, String>();
		put(content, KEY_PAYMENT_ID, paymentId);
		return content;
	}

	/**
	 * The transitional {@code REGISTER_PAYMENT} content: the {@code paymentId} <strong>plus</strong> the fields the
	 * item carried before 2026-09-21, so a robot written against either contract can process it.
	 *
	 * <p>
	 * <strong>This form puts the payee's name, clearing and account number in the Orchestrator queue store, which is
	 * exactly what the paymentId-only contract exists to stop.</strong> It is a bridge for the window where careM has
	 * been released and the robot has not: the robot is moved over to reading
	 * {@code GET .../payments/{paymentId}}, and then {@code financial-assistance.rpa.register-payment.legacy-fields}
	 * is set to {@code false} and the personal data stops being written. Deleting this method is the point.
	 * </p>
	 */
	public static Map<String, String> toLegacyPaymentContent(final String paymentId, final FinalizePayment payment, final int sequence) {
		final var content = toPaymentIdContent(paymentId);
		put(content, KEY_SEQUENCE, sequence);
		ofNullable(payment).ifPresent(source -> {
			put(content, KEY_PAYMENT_DATE, source.getPaymentDate());
			put(content, KEY_AMOUNT, source.getAmount());
			put(content, KEY_CONCERNED_MONTH, source.getConcernedMonth());
			put(content, KEY_ACCOUNTING_CODE, source.getAccountingCode());
			ofNullable(source.getPayee()).ifPresent(payee -> {
				put(content, KEY_PAYEE_NAME, payee.getName());
				put(content, KEY_PAYMENT_METHOD, payee.getPaymentMethod());
				put(content, KEY_CLEARING, payee.getClearing());
				put(content, KEY_ACCOUNT_NUMBER, payee.getAccountNumber());
			});
		});
		return content;
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

	/**
	 * The content of an id-list item ({@code WRITE_MONITORING} / {@code WRITE_JOURNAL} / {@code WRITE_DOCUMENT}): the
	 * ids under {@code key} as a comma-separated list plus their count. The robot fetches each by id.
	 */
	public static Map<String, String> toIdListContent(final String key, final List<String> ids) {
		final var content = new HashMap<String, String>();
		final var values = ofNullable(ids).orElseGet(List::of);
		put(content, key, String.join(",", values));
		put(content, KEY_COUNT, values.size());
		return content;
	}

	/** The comma-separated channel codes for the channels switched on, in a fixed order. */
	public static String toChannelList(final CommunicationChannels channels) {
		final var codes = new ArrayList<String>();
		ofNullable(channels).ifPresent(source -> {
			if (Boolean.TRUE.equals(source.getMinaSidor())) {
				codes.add(CHANNEL_MINA_SIDOR);
			}
			if (Boolean.TRUE.equals(source.getDigitalMailbox())) {
				codes.add(CHANNEL_DIGITAL_MAILBOX);
			}
			if (Boolean.TRUE.equals(source.getLetter())) {
				codes.add(CHANNEL_LETTER);
			}
		});
		return String.join(",", codes);
	}

	public static RpaTask toRpaTask(final RpaAction action, final String reference, final boolean enqueued) {
		return RpaTask.create()
			.withAction(ofNullable(action).map(RpaAction::name).orElse(null))
			.withReference(reference)
			.withEnqueued(enqueued);
	}

	/** Queue item content is a flat string map; nulls are left out rather than sent as "null". */
	private static void put(final Map<String, String> content, final String key, final Object value) {
		ofNullable(value).map(String::valueOf).ifPresent(text -> content.put(key, text));
	}
}
