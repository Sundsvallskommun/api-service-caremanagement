package se.sundsvall.caremanagement.lifecare.service.model;

import java.time.LocalDate;

/**
 * The effective household member for the calculation at commit — the person, the effective number of days in the home
 * (the caseworker value when set, otherwise the process value) and the optional deviation period (included from/to),
 * ready to post to Lifecare FamilyCare {@code CalculationPersons}.
 */
public record EffectivePerson(
	String partyId,
	Integer numberOfDays,
	LocalDate deviationFromDate,
	LocalDate deviationToDate) {
}
