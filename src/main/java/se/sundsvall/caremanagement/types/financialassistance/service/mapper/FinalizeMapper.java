package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.decisions.service.DecisionService.LIFECARE_STATUS_PENDING;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;

/**
 * Mappings for the finalize ("Besluta och utbetala") step: the request → the {@code PAYMENT} decision row, and the
 * request → the entity's audit fields.
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
}
