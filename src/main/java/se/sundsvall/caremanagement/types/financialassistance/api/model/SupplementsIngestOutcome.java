package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The receipt for one delivered item in a supplements ingest. {@code outcome} is {@code CREATED} (new mirror),
 * {@code UPDATED} (existing mirror refreshed), {@code REPLACED} (a full-replace section was rewritten), {@code SKIPPED}
 * (tolerated but not stored — e.g. an unroutable document row or a reminder without its id) or {@code FAILED} (the item
 * could not be processed; the rest of the batch is unaffected).
 */
@Schema(description = "Receipt for one delivered item in a supplements ingest.")
public record SupplementsIngestOutcome(

	@Schema(description = "The envelope section the item came from", examples = "documents", allowableValues = {
		"reminders", "documents", "jobStimulus"
	}) String section,

	@Schema(description = "The item's Lifecare id, when it has one", examples = "27") String lifecareId,

	@Schema(description = "What happened to the item", examples = "CREATED", allowableValues = {
		"CREATED", "UPDATED", "REPLACED", "SKIPPED", "FAILED"
	}) String outcome,

	@Schema(description = "Human-readable detail — the skip/failure reason, or a summary for REPLACED", examples = "3 periods") String detail) {
}
