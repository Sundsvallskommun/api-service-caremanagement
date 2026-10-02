package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.util.List;
import java.util.Map;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebFormField;
import tools.jackson.databind.JsonNode;

/**
 * One ProfessionalWeb call for api-service-lifecare-integrator to make.
 *
 * @param method the HTTP method
 * @param path   the path below the ProfessionalWeb module
 * @param params query parameters, in order
 * @param body   the JSON body, null for none
 */
public record ProfessionalWebExchangeRequest(String method, String path, Map<String, String> params, JsonNode body, List<ProfessionalWebFormField> form) {
}
