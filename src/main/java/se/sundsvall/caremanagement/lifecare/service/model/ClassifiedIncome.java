package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * One SSBTEK income already classified by the operaton regelverk (the {@code Decision_inkomstRalista} DMN): the
 * underlying income plus the verdict — the action ({@code atgard}), the target normberäkning category
 * ({@code normberakning}), whether to flag it ({@code varning}), and the rule note. caremanagement only resolves the
 * category to an FC income-type id and assembles the calculation; it no longer evaluates the regelverk. Deserialised
 * from
 * the {@code classifiedIncomes} JSON the operaton {@code evaluate-income-regelverk} worker produces.
 */
public record ClassifiedIncome(
	SsbtekIncome income,
	String atgard,
	String normberakning,
	boolean varning,
	String regel) {
}
