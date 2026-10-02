package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * How the finalized decision was tied to its beslut in Lifecare.
 *
 * @param decisionId the PAYMENT decision careM recorded
 * @param outcome    REGISTERED when the beslut is in Lifecare and the decision is receipted with it
 * @param lifecareId Lifecare's decisionId
 * @param detail     what went wrong, when something did
 */
@Schema(description = "How the finalized decision was tied to its beslut in Lifecare.", accessMode = READ_ONLY)
public record LifecareDecisionRegistration(
	@Schema(description = "The PAYMENT decision careM recorded", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec") String decisionId,
	@Schema(description = "REGISTERED: the beslut is in Lifecare and the decision is marked SYNCED with its id", examples = "REGISTERED", allowableValues = {
		"REGISTERED", "FAILED", "NOT_SENT"
	}) String outcome,
	@Schema(description = "Lifecare's decisionId", examples = "98") String lifecareId,
	@Schema(description = "What went wrong, when something did") String detail) {
}
