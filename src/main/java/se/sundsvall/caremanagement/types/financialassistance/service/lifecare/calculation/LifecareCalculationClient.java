package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lifecare's normberäkning endpoints, with paths and query strings copied from captures of Lifecare's web app (via
 * Drakel's BFF, 2026-09-24). Every answer and body is a JSON tree: Lifecare's create and update take its own whole
 * object back, and a typed round trip would drop the fields no type knows.
 */
@Component
class LifecareCalculationClient {

	/** Lifecare's businessType for: businessId is an insats. */
	static final String SERVICE_BUSINESS_TYPE = "8";

	/** Lifecare's businessType for: businessId is a beräkning. */
	static final String CALCULATION_BUSINESS_TYPE = "3";

	/**
	 * The print container's owner for a beräkning, as Lifecare's web app asks for it. The owner code is the one the web
	 * app sends for every beräkning; what it stands for is not known.
	 */
	static final String CALCULATION_OWNER_TYPE = "BERAK";
	static final String CALCULATION_OWNER_CODE = "999999999";

	private static final String BUSINESS_TYPE = "businessType";
	private static final String BUSINESS_ID = "businessId";
	private static final String START_DATE = "startDate";
	private static final String END_DATE = "endDate";

	private final ProfessionalWebClient client;
	private final ProfessionalWebProperties properties;

	LifecareCalculationClient(final ProfessionalWebClient client, final ProfessionalWebProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	/**
	 * The underlag for a new beräkning on the insats: a blank one with the household, and the catalogues.
	 *
	 * @param  serviceId the insats
	 * @return           GetProposalService's answer
	 */
	JsonNode readProposal(final int serviceId) {
		return client.get("api2/Calculation/GetProposalService", params(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(serviceId)));
	}

	/**
	 * Every beräkning on the insats, without rows.
	 *
	 * @param  serviceId the insats
	 * @return           ListCalculations' answer, an array
	 */
	JsonNode listForService(final int serviceId) {
		final var params = params(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(serviceId));
		params.put("investigationId", "0");
		params.put("serviceId", String.valueOf(serviceId));
		params.put("onlylatest", "true");
		return client.get("api2/Calculation/ListCalculations", params);
	}

	/**
	 * A saved beräkning as Lifecare shows it: rows, period and its summering.
	 *
	 * @param  calculationId the beräkning
	 * @return               GetCalculation's answer
	 */
	JsonNode read(final int calculationId) {
		return client.get("api2/Calculation/GetCalculation", params(BUSINESS_TYPE, CALCULATION_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(calculationId)));
	}

	/**
	 * A saved beräkning, whole (what Update takes back), with the catalogues.
	 *
	 * @param  calculationId the beräkning
	 * @return               GetCalculationForEdit's answer
	 */
	JsonNode readForEdit(final int calculationId) {
		return client.get("api2/Calculation/GetCalculationForEdit", params(BUSINESS_TYPE, CALCULATION_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(calculationId)));
	}

	/**
	 * The applicant's and co-applicant's jobbstimulans periods on an insats.
	 *
	 * @param  serviceId the insats
	 * @return           GetJobStimulusForService's answer
	 */
	JsonNode readJobStimulus(final int serviceId) {
		return client.get("api2/Calculation/GetJobStimulusForService", params(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(serviceId)));
	}

	/**
	 * Places the members on the norm's rows for the period; Lifecare picks each row and its amount.
	 *
	 * @param  body startDate, endDate, normId and the included calculationPersons
	 * @return      PlacePersons' answer: calculationPersons and, when it sends it, the norm
	 */
	JsonNode placePersons(final ObjectNode body) {
		return client.post("api2/Calculation/PlacePersons", Map.of(), body);
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
		return client.post("api2/Calculation/GetAmount", Map.of(), body).path("amount");
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
		return client.post("api2/Calculation/GetSharedCost", Map.of(), body);
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
		return client.post("api2/Calculation/GetJobStimuliForCalculation", Map.of(), body);
	}

	/**
	 * Creates a beräkning on the insats. Not idempotent: a second call makes a second beräkning, so it is never retried.
	 *
	 * @param  serviceId the insats
	 * @param  body      the Create body
	 * @return           the created beräkning
	 */
	JsonNode create(final int serviceId, final ObjectNode body) {
		return client.post("api2/Calculation/Create", params(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(serviceId)), body);
	}

	/**
	 * Saves changes to a beräkning. Lifecare answers with the beräkning recounted.
	 *
	 * @param  calculationId the beräkning
	 * @param  body          the Update body
	 * @return               the saved beräkning
	 */
	JsonNode update(final int calculationId, final ObjectNode body) {
		return client.post("api2/Calculation/Update", params(BUSINESS_TYPE, CALCULATION_BUSINESS_TYPE, BUSINESS_ID, String.valueOf(calculationId)), body);
	}

	/**
	 * The beräkning rendered as PDF by Lifecare's own print template. Not under api2: it is the page Lifecare's web app
	 * opens to print a beräkning.
	 *
	 * @param  calculationId the beräkning
	 * @return               the PDF
	 */
	byte[] print(final int calculationId) {
		final var params = new LinkedHashMap<String, String>();
		params.put("templateId", properties.calculationPrintTemplateId());
		params.put("ownerType", CALCULATION_OWNER_TYPE);
		params.put("ownerCode", CALCULATION_OWNER_CODE);
		params.put("objectId", String.valueOf(calculationId));
		return client.getPdf("RenderPdf/PrintContainer", params);
	}

	private static LinkedHashMap<String, String> params(final String firstKey, final String firstValue, final String secondKey, final String secondValue) {
		final var params = new LinkedHashMap<String, String>();
		params.put(firstKey, firstValue);
		params.put(secondKey, secondValue);
		return params;
	}
}
