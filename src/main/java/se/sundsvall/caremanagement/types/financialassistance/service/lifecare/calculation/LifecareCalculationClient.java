package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * The normberäkning counts Lifecare makes from bodies careM builds, through lifecare-integrator's resources. Nothing
 * here
 * changes anything in Lifecare.
 */
@Component
class LifecareCalculationClient {

	private static final String START_DATE = "startDate";
	private static final String END_DATE = "endDate";

	private final ProfessionalWebClient client;

	LifecareCalculationClient(final ProfessionalWebClient client) {
		this.client = client;
	}

	/**
	 * A member's amount for the period, as Lifecare counts it from the member's normintervall and days in the household.
	 * The norm row goes along whole, its norm and amounts included.
	 *
	 * @param  person    the member
	 * @param  normRow   the norm row the member is on
	 * @param  startDate the start of the period
	 * @param  endDate   the end of the period
	 * @return           the amount, as Lifecare answered it
	 */
	JsonNode amountFor(final JsonNode person, final JsonNode normRow, final JsonNode startDate, final JsonNode endDate) {
		final var body = JsonNodeFactory.instance.objectNode();
		body.set("person", person);
		body.set("normRow", normRow);
		body.set(START_DATE, startDate);
		body.set(END_DATE, endDate);
		return client.readAmount(body).path("amount");
	}

	/**
	 * The gemensamma kostnader of a household of the norm shared row's size over the period, as Lifecare counts them.
	 *
	 * @param  startDate  the start of the period
	 * @param  endDate    the end of the period
	 * @param  normShared the norm's shared row for the household size
	 * @return            the amount, a bare number
	 */
	JsonNode sharedCost(final JsonNode startDate, final JsonNode endDate, final JsonNode normShared) {
		final var body = JsonNodeFactory.instance.objectNode();
		body.set(START_DATE, startDate);
		body.set(END_DATE, endDate);
		body.set("normShared", normShared);
		return client.readSharedCost(body);
	}

	/**
	 * Marks which members have jobbstimulans during the beräkning's period, as the web app asks before saving.
	 *
	 * @param  calculation the beräkning
	 * @param  jobStimulus the insats's jobbstimulans periods
	 * @return             the beräkning with hasApplicantJobStimuli and hasCoApplicantJobStimuli set
	 */
	JsonNode withJobStimuli(final JsonNode calculation, final JsonNode jobStimulus) {
		final var body = JsonNodeFactory.instance.objectNode();
		body.set("calculation", calculation);
		body.set("jobStimulus", jobStimulus);
		return client.readJobStimuliForCalculation(body);
	}
}
