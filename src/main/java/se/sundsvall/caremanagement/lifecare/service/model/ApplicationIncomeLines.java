package se.sundsvall.caremanagement.lifecare.service.model;

import java.util.List;

/**
 * The application's declared incomes resolved to FamilyCare income types.
 *
 * @param lines          one line per (FamilyCare income type, recipient), amounts summed within the pair
 * @param untransferable the declared incomes no FamilyCare income type in the proposal could take
 */
public record ApplicationIncomeLines(
	List<FamilyCareIncomeLine> lines,
	List<ApplicationIncome> untransferable) {
}
