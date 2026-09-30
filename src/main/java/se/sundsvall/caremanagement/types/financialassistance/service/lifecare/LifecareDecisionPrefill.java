package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionSaveRequest;

/**
 * What Lifecare fills a new beslut in with from a normberäkning saved as final: its TotalSum amount, without sign, and
 * its period.
 *
 * @param calculationId the normberäkning
 * @param amount        the amount
 * @param periodFrom    start of the period
 * @param periodTo      end of the period, null when Lifecare gave none
 */
record LifecareDecisionPrefill(int calculationId, BigDecimal amount, LocalDate periodFrom, LocalDate periodTo) {

	/**
	 * The caseworker's beslut with what they left out taken from the normberäkning: the amount, and the period when they
	 * gave neither end of it. What the caseworker gave always wins, since Lifecare lets a beslut differ from the
	 * normberäkning.
	 *
	 * @param  request the caseworker's beslut
	 * @return         the beslut to save
	 */
	LifecareDecisionSaveRequest fillIn(final LifecareDecisionSaveRequest request) {
		var from = request.periodFrom();
		var to = request.periodTo();
		if (from == null && to == null) {
			from = periodFrom;
			to = periodTo;
		}
		return new LifecareDecisionSaveRequest(request.decisionCode(), request.date(), from, to, Objects.requireNonNullElse(request.amount(), amount), request.reasonCode(),
			request.decisionMessage(), request.writeProtect());
	}
}
