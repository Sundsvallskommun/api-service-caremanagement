package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * What a new beslut gets from the errand's normberäkning, as Lifecare's calculation view fills it in.
 *
 * @param calculationId the errand's normberäkning, absent when none is linked
 * @param prefilled     Lifecare filled amount and period in, which it does only for a normberäkning saved as final
 * @param amount        the normberäkning's TotalSum, without sign
 * @param periodFrom    start of the normberäkning's period
 * @param periodTo      end of the normberäkning's period
 */
@Schema(description = """
	What a new beslut gets from the errand's normberäkning, as Lifecare's calculation view fills it in for Besluta. \
	Lifecare keeps no link between a beslut and a normberäkning; the beslut may carry another amount.""", accessMode = READ_ONLY)
public record LifecareDecisionProposal(
	@Schema(description = "Lifecare's calculationId of the errand's normberäkning; absent when the errand has none linked", examples = "25") Integer calculationId,
	@Schema(description = "Lifecare filled amount and period in, which it does only for a normberäkning saved as final", examples = "true") boolean prefilled,
	@Schema(description = "The normberäkning's result (TotalSum) as the beslut's amount, without sign; absent unless prefilled", examples = "2068") BigDecimal amount,
	@Schema(description = "Start of the normberäkning's period, yyyy-MM-dd; absent unless prefilled", examples = "2026-09-01") String periodFrom,
	@Schema(description = "End of the normberäkning's period, yyyy-MM-dd; absent unless prefilled", examples = "2026-09-30") String periodTo) {
}
