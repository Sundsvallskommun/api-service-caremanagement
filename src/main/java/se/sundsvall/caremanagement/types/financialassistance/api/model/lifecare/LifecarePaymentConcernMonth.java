package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A month an utbetalning on the insats may concern, as Lifecare offers it.
 */
@Schema(description = "A month an utbetalning on the insats may concern, as Lifecare offers it")
public record LifecarePaymentConcernMonth(

	@Schema(description = "The month, yyyy-MM", examples = "2026-09") String month,

	@Schema(description = "Lifecare's own wording", examples = "September 2026") String label) {
}
