package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A betalningsmottagare in Lifecare, with the account and address an utbetalning to it copies. The personnummer the
 * payee is filed under is left behind.
 */
@Schema(description = "A betalningsmottagare in Lifecare, with the account and address an utbetalning to it copies")
public record LifecarePayee(

	@Schema(description = "Lifecare's payee id", examples = "2") Integer id,

	@Schema(description = "The label the payee has in Lifecare's list", examples = "Konto A") String label,

	@Schema(description = "The account holder", examples = "Kontoinnehavare A") String name,

	@Schema(description = "Lifecare's betalsätt code", examples = "14") Integer paymentMethodCode,

	@Schema(description = "The betalsätt's name, as the utbetalning stores it", examples = "Bankgiro via Plusgiro") String paymentMethod,

	@Schema(description = "Clearing number; empty when the betalsätt has none", examples = "6000") String clearing,

	@Schema(description = "Account, bankgiro or plusgiro number; empty for the registered-address entry", examples = "1111-1111") String accountNumber,

	@Schema(description = "Street address", examples = "Storgatan 1") String streetAddress,

	@Schema(description = "c/o address", examples = "") String careOfAddress,

	@Schema(description = "Postal code", examples = "85230") String postalCode,

	@Schema(description = "Postal town", examples = "Sundsvall") String postalAddress,

	@Schema(description = "Lifecare's Adress entry: pays to the client's registered address rather than an account", examples = "false") boolean toRegisteredAddress) {
}
