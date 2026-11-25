package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A konteringsrad on the insats: an ändamål an utbetalning can be booked on.
 */
@Schema(description = "A konteringsrad on the insats: an ändamål an utbetalning can be booked on")
public record LifecarePaymentPosting(

	@Schema(description = "Lifecare's ändamål code, which the utbetalning keeps as its kontering", examples = "1") Integer purpose,

	@Schema(description = "The ändamål's text", examples = "Försörjningsstöd exklusive tillfälligt boende") String text) {
}
