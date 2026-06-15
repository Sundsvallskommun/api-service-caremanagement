package se.sundsvall.caremanagement.lifecare.service.model;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import java.util.List;

/**
 * Result of mapping a set of SSBTEK incomes to FC normberäkning income rows: the rows ready to drop into a
 * {@code PostCalculationBodyRequest.calculationIncomes}, plus the incomes that could not be transferred and must be
 * raised as warnings to the handläggare.
 *
 * @param calculationIncomes the FC income rows to post (one per resolved income type, applicant/co-applicant merged)
 * @param unhandledIncomes   the SSBTEK incomes that were not transferred, with the reason
 */
public record SsbtekIncomeMappingResult(
	List<PersonBasedCalculationIncomePostDTO> calculationIncomes,
	List<UnhandledIncome> unhandledIncomes) {
}
