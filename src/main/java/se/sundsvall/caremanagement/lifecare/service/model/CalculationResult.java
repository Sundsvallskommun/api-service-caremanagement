package se.sundsvall.caremanagement.lifecare.service.model;

import java.util.List;

/**
 * Outcome of building and posting a financial assistance calculation: the created Lifecare calculation id plus the
 * completeness
 * verdict
 * — whether this month's calculation covers every income type the previous month's did, and which types are still
 * missing. The financial assistance process polls SSBTEK daily until {@code informationComplete} so a caseworker
 * reviews a calculation
 * built on the full picture.
 *
 * @param calculationId       the id of the calculation created in Lifecare FC
 * @param informationComplete whether every previous-month income type is present this month (no missing types)
 * @param missingIncomeTypes  the previous-month income types not yet present this month (empty ⇒ complete)
 */
public record CalculationResult(Integer calculationId, boolean informationComplete, List<String> missingIncomeTypes) {
}
