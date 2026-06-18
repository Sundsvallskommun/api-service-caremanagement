package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * The effective expense for one cost at commit — the applied amount (what the citizen asked for) and the approved
 * amount (the handläggare value when set, otherwise the regelverk cap), ready to post to Lifecare FC. {@code costType}
 * is the EB cost type the FC expense-type id is resolved from against the calculation proposal.
 */
public record EffectiveExpense(
	String costType,
	Double appliedAmount,
	Double approvedAmount,
	String note) {
}
