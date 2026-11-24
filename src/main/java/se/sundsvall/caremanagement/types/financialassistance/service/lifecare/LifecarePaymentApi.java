package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;

/**
 * Lifecare's utbetalningar, betalningsmottagare and hushåll, through lifecare-integrator's resources.
 *
 * <p>
 * The answers carry personnummer, names and bank accounts. Nothing here is logged.
 * </p>
 */
@Component
class LifecarePaymentApi {

	private final ProfessionalWebClient client;

	LifecarePaymentApi(final ProfessionalWebClient client) {
		this.client = client;
	}

	/** The underlag for a new utbetalning on the insats: blank payment, payees, betalsätt, saldon and months. */
	JsonNode readPaymentForCreate(final int serviceId) {
		return client.readPaymentProposal(serviceId);
	}

	/** The latest utbetalningar registered on the insats; Lifecare's own word, so the list may not reach far back. */
	JsonNode readLatestPayments(final int serviceId) {
		return client.readLatestPayments(serviceId);
	}

	/**
	 * Whether the person of the insats has a hushåll on the date, the check Lifecare's web app makes before it saves an
	 * utbetalning. An empty answer is no hushåll.
	 */
	boolean hasHouseholdOn(final int serviceId, final String date) {
		return client.hasHousehold(serviceId, date).asBoolean(false);
	}

	/** Registers an utbetalning on the insats. Not idempotent and it moves money: never retry it. */
	JsonNode createPayment(final int serviceId, final JsonNode payment) {
		return client.createPayment(serviceId, payment);
	}

	/** Creates a betalningsmottagare. Not idempotent: a second call creates a second payee. */
	JsonNode createPayee(final JsonNode payee) {
		return client.createPayee(payee);
	}
}
