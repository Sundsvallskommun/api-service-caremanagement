package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An utbetalning registered in Lifecare (not the same as paid out).
 */
@Schema(description = "An utbetalning registered in Lifecare (not the same as paid out)")
public record LifecarePaymentCreated(

	@Schema(description = "Lifecare's id for the new utbetalning", examples = "4") String lifecareId,

	@Schema(description = """
		Whether the utbetalning was linked to the errand (lifecarePaymentIds). False means it is registered in Lifecare \
		but the errand does not point at it; it must not be registered again""", examples = "true") boolean linkedToErrand) {
}
