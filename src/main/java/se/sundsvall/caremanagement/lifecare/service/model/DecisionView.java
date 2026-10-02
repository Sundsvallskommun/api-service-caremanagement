package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * A Lifecare beslut (decision) as read for the handläggare-facing case history — the decision header plus the persons
 * it concerned. A display projection of the generated {@code PersonBasedDecisionDTO}; dates are passed through as the
 * raw Lifecare strings. {@code serviceId} is the Lifecare insats (service) the decision was made under — the
 * discriminator between an ekonomiskt bistånd decision and one from another IFO area (Vux, BoU, LVM …).
 */
public record DecisionView(
	Integer id,
	String date,
	String type,
	String fromDate,
	String toDate,
	String reason,
	String decisionMaker,
	String organization,
	Integer serviceId,
	BigDecimal amount,
	String coApplicant,
	String reasonCoApplicant,
	List<DecisionPersonView> persons) {
}
