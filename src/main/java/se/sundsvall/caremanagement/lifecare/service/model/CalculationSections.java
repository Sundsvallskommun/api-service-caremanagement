package se.sundsvall.caremanagement.lifecare.service.model;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpensePostDTO;
import java.util.List;

/**
 * The row sections of a full FamilyCare calculation body — the effective income, expense, special-expense and
 * household-person rows plus the draft header. Grouped into one carrier so {@code CalculationAssembler.assemble} stays
 * under the parameter limit; any field may be {@code null} (treated as "left unset → FamilyCare default" by the
 * assembler).
 *
 * @param incomes         the effective FamilyCare income rows; may be {@code null}
 * @param expenses        the effective FamilyCare expense rows; may be {@code null}
 * @param specialExpenses the effective FamilyCare special-expense rows; may be {@code null}
 * @param persons         the household person rows; may be {@code null}
 * @param header          the draft header (norm/date/household overrides); may be {@code null}
 */
public record CalculationSections(
	List<PersonBasedCalculationIncomePostDTO> incomes,
	List<PersonBasedCalculationExpensePostDTO> expenses,
	List<PersonBasedCalculationSpecialExpensePostDTO> specialExpenses,
	List<PersonBasedCalculationPersonPostDTO> persons,
	CalculationHeader header) {
}
