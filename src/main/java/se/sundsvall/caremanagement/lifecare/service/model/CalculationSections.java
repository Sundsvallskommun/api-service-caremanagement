package se.sundsvall.caremanagement.lifecare.service.model;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationSpecialExpensePostDTO;
import java.util.List;

/**
 * The row sections of a full FC calculation body — the effective income, expense, special-expense and household-person
 * rows plus the draft header. Grouped into one carrier so {@code CalculationAssembler.assemble} stays under the
 * parameter limit; any field may be {@code null} (treated as "left unset → FC default" by the assembler).
 *
 * @param incomes         the effective FC income rows; may be {@code null}
 * @param expenses        the effective FC expense rows; may be {@code null}
 * @param specialExpenses the effective FC special-expense rows; may be {@code null}
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
