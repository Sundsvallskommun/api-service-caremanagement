package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * The effective expense for one cost at commit — the applied amount (what the citizen asked for) and the approved
 * amount (the caseworker value when set, otherwise the regelverk cap), ready to post to Lifecare FC. {@code costType}
 * is the financial assistance cost type the FC expense-type id is resolved from against the calculation proposal;
 * {@code bucket} routes
 * it
 * to the FC CalculationExpenses ({@code EXPENSE}) or CalculationSpecialExpenses ({@code SPECIAL_EXPENSE}) array.
 */
public record EffectiveExpense(
	String costType,
	String bucket,
	Double appliedAmount,
	Double approvedAmount,
	String note) {
}
