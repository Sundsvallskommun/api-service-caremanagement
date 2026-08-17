package se.sundsvall.caremanagement.lifecare.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One SSBTEK income already classified by the operaton regelverk (the {@code Decision_incomeRalista} DMN): the
 * underlying income plus the verdict — the {@code action} ({@code atgard}), the target {@code calculation} category,
 * whether to {@code warning}-flag it ({@code varning}), and the {@code rule} note ({@code regel}). caremanagement only
 * resolves the category to an FamilyCare income-type id and assembles the calculation; it no longer evaluates the
 * regelverk. Deserialised from the {@code classifiedIncomes} JSON the operaton {@code evaluate-income-regelverk} worker
 * produces — the JSON keys stay Swedish (the DMN contract), mapped onto English record components via {@link
 * JsonProperty}.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	@JsonProperty("atgard") String action,
	@JsonProperty("normberakning") String calculation,
	@JsonProperty("varning") boolean warning,
	@JsonProperty("regel") String rule) {
}
