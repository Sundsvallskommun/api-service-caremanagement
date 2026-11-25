package se.sundsvall.caremanagement.lifecare.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One SSBTEK income already classified by the operaton regelverk (the {@code Decision_incomeRalista} DMN): the
 * underlying income plus the verdict — the {@code action} ({@code atgard}), the target {@code calculation} category,
 * whether to {@code warning}-flag it ({@code varning}), and the {@code rule} note ({@code regel}). caremanagement only
 * resolves the category to a FamilyCare income-type id and assembles the calculation; it no longer evaluates the
 * regelverk. Deserialised from the {@code classifiedIncomes} JSON the operaton {@code evaluate-income-regelverk} worker
 * produces — the JSON keys stay Swedish (the DMN contract), mapped onto English record components via {@link
 * JsonProperty}.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	@JsonProperty("atgard") String action,
	@JsonProperty("normberakning") String calculation,
	@JsonProperty("varning") boolean warning,
	@JsonProperty("regel") String rule,
	@JsonProperty("jamforelseperiod") Boolean fromComparisonPeriod) {

	/**
	 * A control-period income - the ordinary case, never filtered against the previous month.
	 * <p>
	 * The flag is a {@code Boolean} rather than a primitive on purpose: an engine that has not been deployed yet sends
	 * {@code classifiedIncomes} without the key at all, and Jackson refuses to map an absent value onto a primitive.
	 * A nullable flag lets caremanagement read both the old and the new payload, so the two services can be deployed
	 * in either order.
	 */
	public ClassifiedIncome(final SsbtekIncome income, final String action, final String calculation,
		final boolean warning, final String rule) {
		this(income, action, calculation, warning, rule, false);
	}

	/** Whether this income came from the comparison period; absent in payloads from an older engine, meaning no. */
	public boolean isFromComparisonPeriod() {
		return Boolean.TRUE.equals(fromComparisonPeriod);
	}
}
