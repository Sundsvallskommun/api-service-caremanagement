package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.SERVICE_BUSINESS_TYPE;

/**
 * Lifecare's utbetalning endpoints, with paths and query strings copied from captures of its own web app, down to the
 * trailing slash on HasHouseholdThisDate and its absence on Payee/Create: its paths are not consistent, and
 * correcting them breaks them.
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
		return client.get("api2/Payment/GetPaymentForCreate", serviceParams(serviceId));
	}

	/** The latest utbetalningar registered on the insats; Lifecare's own word, so the list may not reach far back. */
	JsonNode readLatestPayments(final int serviceId) {
		return client.get("api2/Payment/GetLatestPayments", serviceParams(serviceId));
	}

	/**
	 * Whether the person has a hushåll on the date, the check Lifecare's web app makes before it saves an utbetalning.
	 * The personnummer goes in the query string because that is where Lifecare reads it.
	 */
	boolean hasHouseholdOn(final String personId, final String date) {
		final var params = new LinkedHashMap<String, String>();
		params.put("personId", personId);
		params.put("date", date);
		return client.get("api2/Household/HasHouseholdThisDate/", params).asBoolean(false);
	}

	/** Registers an utbetalning on the insats. Not idempotent and it moves money: never retry it. */
	JsonNode createPayment(final int serviceId, final JsonNode payment) {
		return client.post("api2/Payment/Create", serviceParams(serviceId), payment);
	}

	/** Creates a betalningsmottagare. Not idempotent: a second call creates a second payee. */
	JsonNode createPayee(final JsonNode payee) {
		return client.post("api2/Payee/Create", Map.of(), payee);
	}

	private static Map<String, String> serviceParams(final int serviceId) {
		final var params = new LinkedHashMap<String, String>();
		params.put("businessType", SERVICE_BUSINESS_TYPE);
		params.put("businessId", String.valueOf(serviceId));
		return params;
	}
}
