package se.sundsvall.caremanagement.lifecare.service.model;

import java.time.LocalDate;

/**
 * The effective household member for the normberäkning at commit — the person, the effective number of days in the home
 * (the handläggare value when set, otherwise the process value) and the optional avvikelseperiod (ingår från/till),
 * ready to post to Lifecare FC {@code CalculationPersons}.
 */
public record EffectivePerson(
	String partyId,
	Integer numberOfDays,
	LocalDate deviationFromDate,
	LocalDate deviationToDate) {
}
