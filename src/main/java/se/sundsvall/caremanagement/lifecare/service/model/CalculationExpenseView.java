package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;

/**
 * A single expense row on a Lifecare normberäkning, as read for display — used for both the regular (UTGIFTER) and the
 * special-expense (LEVNADSKOSTNADER I ÖVRIGT) arrays.
 */
public record CalculationExpenseView(
	String type,
	BigDecimal appliedAmount,
	BigDecimal approvedAmount) {
}
