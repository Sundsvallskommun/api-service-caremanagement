package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;

/**
 * A warning that a förmån's net income changed by more than the threshold between the jämförelseperiod and the
 * kontrollperiod (ssbtek-regelverk.txt: "jämför summering i jämförelseperiod med summering i kontrollperiod. Om det
 * skiljer mer än X% (12 idag) ner eller upp = varning"). Surfaced to the handläggare for review.
 *
 * @param forman        the SSBTEK förmån that changed
 * @param jamforelseSum the summed net amount in the jämförelseperiod (the baseline)
 * @param kontrollSum   the summed net amount in the kontrollperiod (0 if the förmån disappeared)
 * @param changePercent the signed change from jämförelse to kontroll, in percent (negative = decrease)
 */
public record SsbtekChangeWarning(
	String forman,
	BigDecimal jamforelseSum,
	BigDecimal kontrollSum,
	BigDecimal changePercent) {
}
