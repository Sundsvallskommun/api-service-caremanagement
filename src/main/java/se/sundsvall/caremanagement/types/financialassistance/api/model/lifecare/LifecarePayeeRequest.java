package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A new betalningsmottagare, written straight to Lifecare. The betalsätt is Lifecare's own code, from the payment
 * options it lists for the insats; clearing and account number are filled in as the betalsätt needs them.
 */
@Schema(description = "A new betalningsmottagare to add in Lifecare")
public record LifecarePayeeRequest(

	@Schema(description = "The account holder", examples = "Kontoinnehavare B", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String name,

	@Schema(description = "The label in Lifecare's list; the account holder's name when left out", examples = "Konto B") @Size(max = 255) String payeeName,

	@Schema(description = "Lifecare's betalsätt code", examples = "14", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Integer paymentMethod,

	@Schema(description = "Clearing number", examples = "6000") @Size(max = 16) String clearing,

	@Schema(description = "Account, bankgiro or plusgiro number", examples = "22222222") @Size(max = 64) String accountNumber) {
}
