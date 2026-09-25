package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Where the normberäkning saved in Lifecare no longer matches SSBTEK — what Draken's BFF writes into the calculation
 * (the AUTO changes) or shows the caseworker (the CONFIRM ones).
 */
@Schema(description = "Where the normberäkning saved in Lifecare no longer matches the latest SSBTEK answer")
public record SsbtekChanges(

	@Schema(description = "The Lifecare calculation compared (the errand's lifecareCalculationId)", examples = "48213") Integer calculationId,

	@Schema(description = "Whether the calculation is saved as final in Lifecare; if so nothing may be written and every change is CONFIRM", examples = "false") boolean isFinal,

	@Schema(description = "When the calculation was read from Lifecare for this comparison", examples = "2026-09-25T09:30:00+02:00") OffsetDateTime comparedAt,

	@Schema(description = "When SSBTEK was last read for the errand; null when it has not been read since the calculation was linked", examples = "2026-09-25T03:00:00+02:00") OffsetDateTime ssbtekReadAt,

	@ArraySchema(schema = @Schema(implementation = SsbtekChange.class), arraySchema = @Schema(description = "The disagreements, empty when the calculation matches SSBTEK")) List<SsbtekChange> changes) {}
