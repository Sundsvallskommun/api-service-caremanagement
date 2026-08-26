package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The receipt for a supplements ingest — one outcome per delivered item. The delivery succeeds as a whole even when
 * individual items are FAILED (partial success); only a structurally unreadable envelope is rejected outright.
 */
@Schema(description = "Receipt for a supplements ingest — one outcome per delivered item.")
public record SupplementsIngestResult(

	@Schema(description = "One outcome per delivered item, in delivery order") List<SupplementsIngestOutcome> results) {}
