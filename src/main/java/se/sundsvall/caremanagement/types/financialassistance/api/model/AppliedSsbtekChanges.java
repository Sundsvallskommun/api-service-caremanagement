package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * What Draken's BFF wrote into the Lifecare calculation from SSBTEK. careM records it as what the system last wrote, so
 * a later SSBTEK change to the same income is again recognised as untouched by a caseworker.
 */
@Schema(description = "What Draken's BFF wrote into the Lifecare calculation from SSBTEK")
public record AppliedSsbtekChanges(

	@Schema(description = "The Lifecare calculation written to — must be the errand's lifecareCalculationId", examples = "48213", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Integer calculationId,

	@ArraySchema(schema = @Schema(implementation = AppliedSsbtekChange.class),
		arraySchema = @Schema(description = "The incomes written",
			requiredMode = Schema.RequiredMode.REQUIRED)) @NotEmpty List<@Valid @NotNull AppliedSsbtekChange> applied) {}
