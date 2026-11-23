package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the Lifecare ProfessionalWeb integration. How Lifecare is reached is lifecare-integrator's
 * business; what is left here is what careM puts in the calls.
 *
 * <p>
 * {@code testDecisionMaker}, when set, is used as the decision maker on every decision instead of the signed-in
 * caseworker. It exists for test environments where the caseworkers are not yet in Lifecare's list of decision makers.
 * </p>
 *
 * @param decisionPrintTemplateId    the print template of a decision PDF
 * @param calculationPrintTemplateId the print template of a calculation PDF
 * @param documentPrintTemplateId    the print template a written document or blankett is printed with. Lifecare ties
 *                                   print templates to the document's owner (utredning, insats, aktualisering), and
 *                                   refuses a template the owner lacks
 * @param testDecisionMaker          a decision maker to use instead of the signed-in caseworker, test only
 */
@ConfigurationProperties("integration.lifecare-professionalweb")
public record ProfessionalWebProperties(
	@DefaultValue("885bfb68-c97b-47c0-921d-ef00caaa2423") String decisionPrintTemplateId,
	@DefaultValue("7936e9ec-e32a-40cf-9dea-d95b16e021f0") String calculationPrintTemplateId,
	@DefaultValue("b9f20b49-c8ba-4c44-ad33-b2bf8f0b688a") String documentPrintTemplateId,
	String testDecisionMaker) {
}
