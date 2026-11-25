package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * A saldo on the insats: what the beslut granted, what is already booked and what is left.
 */
@Schema(description = "A saldo on the insats: what the beslut granted, what is already booked and what is left")
public record LifecarePaymentBalance(

	@Schema(description = "The saldo's name", examples = "Ek. Bistånd") String name,

	@Schema(description = "What the beslut granted", examples = "4000") BigDecimal approvedAmount,

	@Schema(description = "What is already booked", examples = "1000") BigDecimal bookedAmount,

	@Schema(description = "What is left to pay out", examples = "3000") BigDecimal balanceAmount) {
}
