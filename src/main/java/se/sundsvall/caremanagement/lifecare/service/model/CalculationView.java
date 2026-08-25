package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * A Lifecare normberäkning (calculation) as read for the handläggare-facing case history — the header sums plus the
 * full income, expense, special-expense and household-member breakdown. A display projection of the generated
 * {@code PersonBasedCalculationDTO}; dates are passed through as the raw Lifecare strings.
 */
public record CalculationView(
	Integer id,
	String norm,
	String fromDate,
	String toDate,
	BigDecimal incomeSum,
	BigDecimal expenseSum,
	BigDecimal specialExpenseSum,
	BigDecimal normSum,
	BigDecimal commonHouseholdCost,
	BigDecimal familyCost,
	BigDecimal balance,
	BigDecimal totalSum,
	Boolean isFinal,
	List<CalculationPersonView> persons,
	List<CalculationIncomeView> incomes,
	List<CalculationExpenseView> expenses,
	List<CalculationExpenseView> specialExpenses) {
}
