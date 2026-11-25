package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Whether the Lifecare utbetalning for the errand's application month has been registered.
 */
@Schema(description = "Whether the Lifecare utbetalning for the errand's application month has been registered")
public record LifecarePaymentStatus(

	@Schema(description = "The application month (yyyy-MM) the status concerns", examples = "2026-09") String applicationMonth,

	@Schema(description = "True when a standing Lifecare utbetalning concerning the application month is registered", examples = "true") boolean effectuated,

	@Schema(description = "The date of that utbetalning (Lifecare PayDate), when effectuated", examples = "2026-09-21") String paymentDate,

	@Schema(description = "The amount of that utbetalning, when effectuated", examples = "3000") BigDecimal amount,

	@Schema(description = "Lifecare's own status for that utbetalning, when effectuated", examples = "Utbetald") String status,

	@Schema(description = "True when the status could not be determined (no application month, no insats, or Lifecare unavailable)", examples = "false") boolean unavailable) {
}
