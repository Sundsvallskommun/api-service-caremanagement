package se.sundsvall.caremanagement.lifecare.service.model;

import java.util.List;

/**
 * The outcome of building and posting an SSBTEK-driven FC normberäkning: the FC calculation id that was created, plus
 * the income warnings the handläggare must review — the incomes Drakel could not auto-transfer, and the förmåner whose
 * net income changed beyond the threshold between the periods.
 *
 * @param calculationId    the id of the normberäkning created in Lifecare FC
 * @param unhandledIncomes SSBTEK incomes not transferred (unknown förmån / FC type not offered)
 * @param changeWarnings   förmåner whose net income changed beyond the threshold between the periods
 */
public record NormberakningResult(
	Integer calculationId,
	List<UnhandledIncome> unhandledIncomes,
	List<SsbtekChangeWarning> changeWarnings) {
}
