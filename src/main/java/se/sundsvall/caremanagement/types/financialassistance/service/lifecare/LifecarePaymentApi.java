package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;

/**
 * Lifecare's hushåll check, through lifecare-integrator's resources.
 */
@Component
class LifecarePaymentApi {

	private final ProfessionalWebClient client;

	LifecarePaymentApi(final ProfessionalWebClient client) {
		this.client = client;
	}

	/**
	 * Whether the person of the insats has a hushåll on the date, the check Lifecare's web app makes before it saves an
	 * utbetalning. An empty answer is no hushåll.
	 */
	boolean hasHouseholdOn(final int serviceId, final String date) {
		return client.hasHousehold(serviceId, date).asBoolean(false);
	}
}
