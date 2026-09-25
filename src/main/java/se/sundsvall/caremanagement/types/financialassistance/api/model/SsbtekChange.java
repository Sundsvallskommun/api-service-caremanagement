package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * One income on which the latest SSBTEK answer and the normberäkning saved in Lifecare disagree, and whether Draken's
 * BFF may bring the calculation in line without asking.
 */
@Schema(description = "One income on which the latest SSBTEK answer and the normberäkning saved in Lifecare disagree")
public record SsbtekChange(

	@Schema(description = """
		ADD = SSBTEK reports an income the calculation lacks; CHANGE = the calculation has the income at another amount; GONE = an \
		income the system wrote to the calculation is no longer reported by SSBTEK""", examples = "CHANGE", allowableValues = {
		"ADD", "CHANGE", "GONE"
	}) String kind,

	@Schema(description = """
		AUTO = the calculation still holds what the system last wrote there (no caseworker has touched the income), so the SSBTEK \
		amount may be written without asking; CONFIRM = show it to the caseworker and write only on their say-so""",
		examples = "AUTO",
		allowableValues = {
			"AUTO", "CONFIRM"
		}) String mode,

	@Schema(description = """
		Why the change needs confirming; null for AUTO. FINAL = the calculation is final and takes no change; NO_BASELINE = careM has \
		no record of what the system wrote to this calculation; EDITED = a caseworker has changed the amount; REMOVED = the income \
		was taken out of the calculation (or never put in) on purpose; MULTIPLE_ROWS = the type has several rows; GONE_FROM_SSBTEK = \
		SSBTEK no longer reports it, which does not prove it has stopped""", examples = "EDITED", allowableValues = {
		"FINAL", "NO_BASELINE", "EDITED", "REMOVED", "MULTIPLE_ROWS", "GONE_FROM_SSBTEK"
	}) String reason,

	@Schema(description = "Whose income: the applicant (Lifecare's S column) or the co-applicant (M)", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	}) String role,

	@Schema(description = "The Lifecare income type id, when careM knows it (always for ADD)", examples = "12") Integer incomeTypeId,

	@Schema(description = "The Lifecare income type name — what the calculation row is matched on", examples = "Lön") String incomeType,

	@Schema(description = "The amount SSBTEK gives; null for GONE", examples = "12400.00") BigDecimal ssbtekAmount,

	@Schema(description = "The amount in the calculation (all rows of the type summed); null for ADD", examples = "11900.00") BigDecimal lifecareAmount) {
}
