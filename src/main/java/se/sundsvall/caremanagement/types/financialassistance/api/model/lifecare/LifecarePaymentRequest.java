package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * The utbetalning form's values, registered straight in Lifecare. The payee goes as its fields (name, account and
 * address), the way Lifecare's own utbetalning copies them from the chosen betalningsmottagare. Every field is
 * optional here; what the utbetalning cannot do without is refused with a reason when it is built.
 */
@Schema(description = "An utbetalning to register on the errand's insats in Lifecare")
public record LifecarePaymentRequest(

	@Schema(description = "Utbetalningsdatum, yyyy-MM-dd; Lifecare's proposed date when left out", examples = "2026-09-21") @Pattern(regexp = DATE, message = "must be a date as yyyy-MM-dd") String paymentDate,

	@Schema(description = "The amount; must be above 0", examples = "3000") BigDecimal amount,

	@Schema(description = "The month the utbetalning concerns, yyyy-MM", examples = "2026-09") @Pattern(regexp = MONTH, message = "must be a month as yyyy-MM") String applicationMonth,

	@Schema(description = "The betalsätt's name, matched against Lifecare's betalsätt on the insats", examples = "Bankgiro via Plusgiro") @Size(max = 255) String paymentMethod,

	@Schema(description = "The payee's name", examples = "Kontoinnehavare A") @Size(max = 255) String payeeName,

	@Schema(description = "The payee's street address", examples = "Storgatan 1") @Size(max = 255) String payeeAddress,

	@Schema(description = "The payee's c/o address", examples = "") @Size(max = 255) String payeeCareOf,

	@Schema(description = "The payee's postal code", examples = "85230") @Size(max = 16) String payeeZipCode,

	@Schema(description = "The payee's postal town", examples = "Sundsvall") @Size(max = 255) String payeeCity,

	@Schema(description = "Clearing number", examples = "6000") @Size(max = 16) String clearingNumber,

	@Schema(description = "Account number; left out for the registered-address payee", examples = "11111111") @Size(max = 64) String accountNumber,

	@Schema(description = "Kontering: the ändamål (Lifecare purpose) among the insats's konteringsrader", examples = "1") @Size(max = 64) String accountingCode,

	@Schema(description = "Lokalbetalningsnummer", examples = "") @Size(max = 64) String localPaymentNumber,

	@Schema(description = "Invoice number (Lifecare billingNumber)", examples = "123") @Size(max = 64) String invoiceNumber,

	@Schema(description = "Whether the invoice number is an OCR number", examples = "false") Boolean usesOcr,

	@ArraySchema(arraySchema = @Schema(description = "Message rows, at most seven"), schema = @Schema(maxLength = 255)) @Size(max = 7) List<@Size(max = 255) String> messageLines) {

	static final String DATE = "^\\d{4}-\\d{2}-\\d{2}$";
	static final String MONTH = "^\\d{4}-(0[1-9]|1[0-2])$";
}
