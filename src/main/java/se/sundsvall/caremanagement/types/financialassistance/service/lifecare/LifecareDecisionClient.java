package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import tools.jackson.databind.JsonNode;

/**
 * Lifecare's beslut endpoints, with paths and query strings as Lifecare's own web app sends them (captures of
 * 2026-09-23).
 *
 * <p>
 * Every answer carries the personnummer of the persons the beslut concerns: never log one.
 * </p>
 */
@Component
class LifecareDecisionClient {

	/** Lifecare's businessType for a businessId that is an insats. */
	static final String SERVICE_BUSINESS_TYPE = "8";

	/** Lifecare's businessType for a businessId that is a beslut. */
	static final String DECISION_BUSINESS_TYPE = "4";

	static final String PATH_PROPOSAL = "api2/Decision/GetProposalForService";
	static final String PATH_REASONS = "api2/Decision/GetMappedDecisionReasons";
	static final String PATH_DECISION = "api2/Decision/GetDecision";
	static final String PATH_CREATE = "api2/Decision/Create";
	static final String PATH_UPDATE = "api2/Decision/Update";
	static final String PATH_PRINT = "RenderPdf/PrintDecision";

	private static final String BUSINESS_TYPE = "businessType";
	private static final String BUSINESS_ID = "businessId";

	private final ProfessionalWebClient client;
	private final ProfessionalWebProperties properties;

	LifecareDecisionClient(final ProfessionalWebClient client, final ProfessionalWebProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	/**
	 * The underlag for a new beslut on the insats: the blank beslut, the beslutstyper and the beslutsfattare.
	 *
	 * @param  serviceId the insats
	 * @return           the underlag
	 */
	JsonNode readProposal(final int serviceId) {
		final var params = new LinkedHashMap<String, String>();
		params.put(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE);
		params.put(BUSINESS_ID, String.valueOf(serviceId));
		params.put("amountType", "");
		params.put("calculationId", "0");
		params.put("proposalId", "0");
		return client.get(PATH_PROPOSAL, params);
	}

	/**
	 * Lifecare's orsak catalogue for a beslutstyp. The id is the beslutstyp's code, not an insats.
	 *
	 * @param  decisionCode the beslutstyp
	 * @return              the catalogue, a tree of headings and orsaker
	 */
	JsonNode readReasons(final int decisionCode) {
		return client.get(PATH_REASONS, Map.of("id", String.valueOf(decisionCode)));
	}

	/**
	 * A beslut registered in Lifecare, whole: the object an update takes back.
	 *
	 * @param  decisionId the beslut
	 * @return            the beslut
	 */
	JsonNode readDecision(final int decisionId) {
		return client.get(PATH_DECISION, decisionParams(decisionId));
	}

	/**
	 * The beslut rendered as PDF by Lifecare's own print template.
	 *
	 * @param  decisionId the beslut
	 * @return            the PDF
	 */
	byte[] printDecision(final int decisionId) {
		final var params = new LinkedHashMap<String, String>();
		params.put("templateId", properties.decisionPrintTemplateId());
		params.put("decisionId", String.valueOf(decisionId));
		params.put("hideRevisions", "true");
		return client.getPdf(PATH_PRINT, params);
	}

	/**
	 * Registers a beslut on the insats. Not idempotent: a second call makes a second beslut.
	 *
	 * @param  serviceId the insats
	 * @param  body      the underlag's blank beslut, filled in
	 * @return           the created beslut, carrying its decisionId
	 */
	JsonNode create(final int serviceId, final JsonNode body) {
		final var params = new LinkedHashMap<String, String>();
		params.put(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE);
		params.put(BUSINESS_ID, String.valueOf(serviceId));
		return client.post(PATH_CREATE, params, body);
	}

	/**
	 * Saves changes to a registered beslut.
	 *
	 * @param  decisionId the beslut
	 * @param  body       the read beslut with the changes applied
	 * @return            the beslut as it now stands
	 */
	JsonNode update(final int decisionId, final JsonNode body) {
		return client.post(PATH_UPDATE, decisionParams(decisionId), body);
	}

	private static Map<String, String> decisionParams(final int decisionId) {
		final var params = new LinkedHashMap<String, String>();
		params.put(BUSINESS_TYPE, DECISION_BUSINESS_TYPE);
		params.put(BUSINESS_ID, String.valueOf(decisionId));
		return params;
	}
}
