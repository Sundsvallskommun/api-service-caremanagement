package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * What careM puts in the ProfessionalWeb calls; how Lifecare is reached, and the print templates, are configured in
 * lifecare-integrator.
 *
 * @param testDecisionMaker used as the decision maker on every decision instead of the signed-in caseworker, for test
 *                          environments whose caseworkers are not yet in Lifecare's list of decision makers
 */
@ConfigurationProperties("integration.lifecare-professionalweb")
public record ProfessionalWebProperties(String testDecisionMaker) {
}
