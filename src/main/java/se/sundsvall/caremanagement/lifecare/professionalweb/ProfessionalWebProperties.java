package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Lifecare ProfessionalWeb integration. How Lifecare is reached, and the print templates, are
 * lifecare-integrator's business; what is left here is what careM puts in the calls.
 *
 * <p>
 * {@code testDecisionMaker}, when set, is used as the decision maker on every decision instead of the signed-in
 * caseworker. It exists for test environments where the caseworkers are not yet in Lifecare's list of decision makers.
 * </p>
 *
 * @param testDecisionMaker a decision maker to use instead of the signed-in caseworker, test only
 */
@ConfigurationProperties("integration.lifecare-professionalweb")
public record ProfessionalWebProperties(String testDecisionMaker) {
}
