package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.SERVICE_BUSINESS_TYPE;

/**
 * Lifecare's jobbstimulans endpoints, with paths and query strings copied from captures of its own web app. The
 * answers carry personnummer; nothing here is logged.
 */
@Component
class LifecareJobStimulusApi {

	private final ProfessionalWebClient client;

	LifecareJobStimulusApi(final ProfessionalWebClient client) {
		this.client = client;
	}

	/** The sökandes and medsökandes jobbstimulans periods on an insats. */
	JsonNode readForService(final int serviceId) {
		final var params = new LinkedHashMap<String, String>();
		params.put("businessType", SERVICE_BUSINESS_TYPE);
		params.put("businessId", String.valueOf(serviceId));
		return client.get("api2/Calculation/GetJobStimulusForService", params);
	}

	/**
	 * The end Lifecare gives a period starting on the date: its two-year rule, asked of the server so careM follows the
	 * same rule as Lifecare. The answer is a bare JSON string.
	 */
	JsonNode readToDate(final String fromDate) {
		return client.get("api2/Calculation/GetJobStimulusToDate", Map.of("id", fromDate));
	}

	/** Saves a person's periods. Replaces the whole set: a period left out is deleted. */
	JsonNode save(final JsonNode body) {
		return client.post("api2/Calculation/SaveJobStimulus", Map.of(), body);
	}
}
