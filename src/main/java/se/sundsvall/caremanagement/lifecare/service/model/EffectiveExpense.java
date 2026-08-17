package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * The effective expense for one cost at commit — the applied amount (what the citizen asked for) and the approved
 * amount (the caseworker value when set, otherwise the regelverk cap), ready to post to Lifecare FamilyCare.
 * {@code costType}
 * is the financial assistance cost type the FamilyCare expense-type id is resolved from against the calculation
 * proposal;
 * {@code bucket} routes
 * it
 * to the FamilyCare CalculationExpenses ({@code EXPENSE}) or CalculationSpecialExpenses ({@code SPECIAL_EXPENSE})
 * array.
 */
public record EffectiveExpense(
	String costType,
	String bucket,
	Double appliedAmount,
	Double approvedAmount,
	String note) {
}
