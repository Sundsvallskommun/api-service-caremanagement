package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;

/**
 * Lifecare's jobbstimulans, through lifecare-integrator's resources. The answers carry personnummer; nothing here is
 * logged.
 */
@Component
class LifecareJobStimulusApi {

	private final ProfessionalWebClient client;

	LifecareJobStimulusApi(final ProfessionalWebClient client) {
		this.client = client;
	}

	/** The sökandes and medsökandes jobbstimulans periods on an insats. */
	JsonNode readForService(final int serviceId) {
		return client.readJobStimulus(serviceId);
	}

	/**
	 * The end Lifecare gives a period starting on the date: its two-year rule, asked of the server so careM follows the
	 * same rule as Lifecare. The answer is a bare JSON string.
	 */
	JsonNode readToDate(final String fromDate) {
		return client.readJobStimulusEndDate(fromDate);
	}

	/** Saves a person's periods. Replaces the whole set: a period left out is deleted. */
	JsonNode save(final JsonNode body) {
		return client.saveJobStimulus(body);
	}
}
