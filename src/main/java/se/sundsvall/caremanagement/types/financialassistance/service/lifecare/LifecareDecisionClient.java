package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;

/**
 * The underlag for a beslut made from a normberäkning, through lifecare-integrator's resources. Every answer carries
 * the
 * personnummer of the persons the beslut concerns: never log one.
 */
@Component
class LifecareDecisionClient {

	/**
	 * The amountType Lifecare's calculation view sends when a beslut is made from a normberäkning: its "Besluta" button
	 * at Resultat, the only one of the three Sundsvall's configuration shows (capture 2026-09-30).
	 */
	static final String AMOUNT_TYPE_TOTAL_SUM = "TotalSum";

	private final ProfessionalWebClient client;

	LifecareDecisionClient(final ProfessionalWebClient client) {
		this.client = client;
	}

	/**
	 * When the normberäkning is saved as final, Lifecare fills the blank beslut in with its period and its TotalSum amount,
	 * without sign; otherwise the answer is the same as for a beslut made without one. Lifecare keeps no link between the
	 * beslut and the normberäkning.
	 */
	JsonNode readProposal(final int serviceId, final int calculationId) {
		return client.readDecisionProposal(serviceId, AMOUNT_TYPE_TOTAL_SUM, calculationId);
	}
}
