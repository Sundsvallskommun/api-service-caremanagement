package se.sundsvall.caremanagement.lifecare.service.model;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import java.util.List;

/**
 * The SSBTEK income preparation for a normberäkning: the FC income rows ready to drop into a
 * {@code PostCalculationBodyRequest.calculationIncomes}, the incomes that could not be transferred (handläggare
 * warnings), and the significant period-over-period changes flagged for review. The caller assembles the rest of the
 * {@code PostCalculationBodyRequest} (norm, service/investigation/aktualisering links, household, dates) from the
 * errand context and posts it.
 *
 * @param calculationIncomes the FC income rows to post
 * @param unhandledIncomes   SSBTEK incomes not transferred (unknown förmån / FC type not offered)
 * @param changeWarnings     förmåner whose net income changed beyond the threshold between the periods
 */
public record PreparedSsbtekIncomes(
	List<PersonBasedCalculationIncomePostDTO> calculationIncomes,
	List<UnhandledIncome> unhandledIncomes,
	List<SsbtekChangeWarning> changeWarnings) {
}
