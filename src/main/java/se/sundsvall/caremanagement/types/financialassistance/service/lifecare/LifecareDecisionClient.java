package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import tools.jackson.databind.JsonNode;

/**
 * Lifecare's beslut, through lifecare-integrator's resources.
 *
 * <p>
 * Every answer carries the personnummer of the persons the beslut concerns: never log one.
 * </p>
 */
@Component
class LifecareDecisionClient {

	/**
	 * The amountType Lifecare's calculation view sends when a beslut is made from a normberäkning: its "Besluta" button
	 * at Resultat, the only one of the three Sundsvall's configuration shows (capture 2026-09-30).
	 */
	static final String AMOUNT_TYPE_TOTAL_SUM = "TotalSum";

	static final String PATH_PRINT = "RenderPdf/PrintDecision";

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
		return client.readDecisionProposal(serviceId, "", 0);
	}

	/**
	 * The underlag for a new beslut made from a normberäkning, as Lifecare's calculation view asks for it (capture
	 * 2026-09-30). When the normberäkning is saved as final, Lifecare fills the blank beslut in with its period and its
	 * TotalSum amount, without sign; otherwise the answer is the same as {@link #readProposal(int)}. Lifecare keeps no
	 * link between the beslut and the normberäkning.
	 *
	 * @param  serviceId     the insats
	 * @param  calculationId the normberäkning
	 * @return               the underlag
	 */
	JsonNode readProposal(final int serviceId, final int calculationId) {
		return client.readDecisionProposal(serviceId, AMOUNT_TYPE_TOTAL_SUM, calculationId);
	}

	/**
	 * Lifecare's orsak catalogue for a beslutstyp. The id is the beslutstyp's code, not an insats.
	 *
	 * @param  decisionCode the beslutstyp
	 * @return              the catalogue, a tree of headings and orsaker
	 */
	JsonNode readReasons(final int decisionCode) {
		return client.readDecisionReasons(decisionCode);
	}

	/**
	 * A beslut registered in Lifecare, whole: the object an update takes back.
	 *
	 * @param  decisionId the beslut
	 * @return            the beslut
	 */
	JsonNode readDecision(final int decisionId) {
		return client.readDecision(decisionId);
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
		return client.createDecision(serviceId, body);
	}

	/**
	 * Saves changes to a registered beslut.
	 *
	 * @param  decisionId the beslut
	 * @param  body       the read beslut with the changes applied
	 * @return            the beslut as it now stands
	 */
	JsonNode update(final int decisionId, final JsonNode body) {
		return client.updateDecision(decisionId, body);
	}
}
