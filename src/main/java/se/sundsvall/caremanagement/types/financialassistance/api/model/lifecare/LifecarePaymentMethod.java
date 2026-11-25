package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A betalsätt the errand's insats offers, as Lifecare lists it.
 */
@Schema(description = "A betalsätt the insats offers, as Lifecare lists it")
public record LifecarePaymentMethod(

	@Schema(description = "Lifecare's betalsätt code", examples = "14") Integer code,

	@Schema(description = "The betalsätt's name", examples = "Bankgiro via Plusgiro") String name,

	@Schema(description = "Whether Lokalbetalningsnummer is open for this betalsätt", examples = "false") boolean localNumberEnabled,

	@Schema(description = "Whether Lokalbetalningsnummer must be filled in for this betalsätt", examples = "false") boolean localNumberMandatory) {
}
