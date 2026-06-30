package se.sundsvall.caremanagement.lifecare.service.model;

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
	Double incomeSum,
	Double expenseSum,
	Double specialExpenseSum,
	Double normSum,
	Double commonHouseholdCost,
	Double familyCost,
	Double balance,
	Double totalSum,
	Boolean isFinal,
	List<CalculationPersonView> persons,
	List<CalculationIncomeView> incomes,
	List<CalculationExpenseView> expenses,
	List<CalculationExpenseView> specialExpenses) {
}
