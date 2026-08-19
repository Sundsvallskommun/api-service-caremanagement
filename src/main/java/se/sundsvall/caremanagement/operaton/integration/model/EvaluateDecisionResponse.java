package se.sundsvall.caremanagement.operaton.integration.model;

import java.util.List;
import java.util.Map;

/**
 * Response body from operaton's decision-evaluate endpoint — the DMN result rows, one map per matched rule (output name
 * → value). Defined here (not generated) because the endpoint is not in caremanagement's operaton OpenAPI surface.
 */
public record EvaluateDecisionResponse(List<Map<String, Object>> results) {
}
