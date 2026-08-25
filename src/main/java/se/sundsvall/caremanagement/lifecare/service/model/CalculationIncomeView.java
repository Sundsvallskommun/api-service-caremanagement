package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;

/**
 * A single income row on a Lifecare normberäkning, as read for display. Dates are passed through as the raw Lifecare
 * strings.
 */
public record CalculationIncomeView(
	String type,
	BigDecimal amountApplicant,
	String applicantSearchDate,
	BigDecimal amountCoApplicant,
	String coApplicantSearchDate) {
}
