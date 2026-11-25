package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * What the utbetalning form starts from: Lifecare's own figures, all changeable.
 */
@Schema(description = "What the utbetalning form starts from: Lifecare's own figures, all changeable")
public record LifecarePaymentProposal(

	@Schema(description = "Lifecare's proposed utbetalningsdatum", examples = "2026-09-23") String paymentDate,

	@Schema(description = "The first month Lifecare lets an utbetalning concern, yyyy-MM", examples = "2026-09") String concernedMonth,

	@Schema(description = "What is left on the insats's saldon; absent when nothing is left", examples = "3000") BigDecimal amount,

	@Schema(description = "The payee the latest standing utbetalning on the insats went to, when it is still an active payee", examples = "2") Integer payeeId) {
}
