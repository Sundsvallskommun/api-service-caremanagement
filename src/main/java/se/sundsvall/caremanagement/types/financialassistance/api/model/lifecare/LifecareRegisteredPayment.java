package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * An utbetalning registered on the insats in Lifecare. The account number is left behind.
 */
@Schema(description = "An utbetalning registered on the insats in Lifecare")
public record LifecareRegisteredPayment(

	@Schema(description = "Lifecare's paymentId", examples = "4") Integer id,

	@Schema(description = "The payment date", examples = "2026-09-21") String payDate,

	@Schema(description = "Avser månad, yyyy-MM", examples = "2026-09") String concernedMonth,

	@Schema(description = "The amount", examples = "3000") BigDecimal amount,

	@Schema(description = "The betalsätt's name", examples = "Bankgiro via Plusgiro") String paymentMethod,

	@Schema(description = "Who it goes to", examples = "Kontoinnehavare A") String recipient,

	@Schema(description = "Lifecare's own status", examples = "Utbetald") String status,

	@Schema(description = "Makulerad in Lifecare", examples = "false") boolean cancelled) {
}
