package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * A single income row on a Lifecare normberäkning, as read for display. Dates are passed through as the raw Lifecare
 * strings.
 */
public record CalculationIncomeView(
	String type,
	Double amountApplicant,
	String applicantSearchDate,
	Double amountCoApplicant,
	String coApplicantSearchDate) {
}
