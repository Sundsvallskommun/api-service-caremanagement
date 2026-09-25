package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * One ProfessionalWeb call for api-service-lifecare-integrator to make.
 *
 * @param method the HTTP method
 * @param path   the path below the ProfessionalWeb module
 * @param params query parameters, in order
 * @param body   the JSON body, null for none
 */
public record ProfessionalWebExchangeRequest(String method, String path, Map<String, String> params, JsonNode body) {
}
