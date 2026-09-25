package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * The utbetalning form's values, registered straight in Lifecare. The payee goes as its fields (name, account and
 * address), the way Lifecare's own utbetalning copies them from the chosen betalningsmottagare. Every field is
 * optional here; what the utbetalning cannot do without is refused with a reason when it is built.
 */
@Schema(description = "An utbetalning to register on the errand's insats in Lifecare")
public record LifecarePaymentRequest(

	@Schema(description = "Utbetalningsdatum; Lifecare's proposed date when left out", examples = "2026-09-21") String paymentDate,

	@Schema(description = "The amount; must be above 0", examples = "3000") BigDecimal amount,

	@Schema(description = "The month the utbetalning concerns, yyyy-MM", examples = "2026-09") String applicationMonth,

	@Schema(description = "The betalsätt's name, matched against Lifecare's betalsätt on the insats", examples = "Bankgiro via Plusgiro") String paymentMethod,

	@Schema(description = "The payee's name", examples = "Kontoinnehavare A") String payeeName,

	@Schema(description = "The payee's street address", examples = "Storgatan 1") String payeeAddress,

	@Schema(description = "The payee's c/o address", examples = "") String payeeCareOf,

	@Schema(description = "The payee's postal code", examples = "85230") String payeeZipCode,

	@Schema(description = "The payee's postal town", examples = "Sundsvall") String payeeCity,

	@Schema(description = "Clearing number", examples = "6000") String clearingNumber,

	@Schema(description = "Account number; left out for the registered-address payee", examples = "11111111") String accountNumber,

	@Schema(description = "Kontering: the ändamål (Lifecare purpose) among the insats's konteringsrader", examples = "1") String accountingCode,

	@Schema(description = "Lokalbetalningsnummer", examples = "") String localPaymentNumber,

	@Schema(description = "Invoice number (Lifecare billingNumber)", examples = "123") String invoiceNumber,

	@Schema(description = "Whether the invoice number is an OCR number", examples = "false") Boolean usesOcr,

	@Schema(description = "Message rows, at most seven are sent") List<String> messageLines) {
}
