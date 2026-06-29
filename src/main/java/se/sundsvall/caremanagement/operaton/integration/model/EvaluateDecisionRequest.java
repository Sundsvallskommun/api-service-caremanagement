package se.sundsvall.caremanagement.operaton.integration.model;

import java.util.Map;

/**
 * Request body for operaton's decision-evaluate endpoint
 * ({@code POST /{municipalityId}/decision-definitions/{key}/evaluate}):
 * the input variables the DMN is evaluated against. Defined here (not generated) because the endpoint is not in
 * caremanagement's operaton OpenAPI surface.
 */
public record EvaluateDecisionRequest(Map<String, Object> variables) {
}
