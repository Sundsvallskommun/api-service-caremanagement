package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * One period Arbetsförmedlingen reports an ekonomiskt beslut for — SSBTEK's
 * {@code af.Svar.BeslutInfo.EkonomiskaBeslut.Beslut} ({@code BeslutFrom}/{@code BeslutTom}). The first question of the
 * dagersättning day check (verksamhetens svar 2026-09-23 §2).
 */
@Schema(description = "A period Arbetsförmedlingen reports an economic decision (ekonomiskt beslut) for, as read from SSBTEK.")
public record EconomicDecisionPeriod(

	@Schema(description = "Decision period start (BeslutFrom)", examples = "2026-08-01") LocalDate fromDate,

	@Schema(description = "Decision period end (BeslutTom); null for an open period", examples = "2026-12-31") LocalDate toDate) {
}
