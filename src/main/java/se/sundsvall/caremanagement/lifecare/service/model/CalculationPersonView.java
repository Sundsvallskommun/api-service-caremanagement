package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;

/**
 * A single household member on a Lifecare normberäkning, as read for display. Named, not identified: the personnummer
 * FamilyCare returns (a party id on the integrator route) is left behind, so the history carries no identity that
 * differs by route.
 */
public record CalculationPersonView(
	String name,
	BigDecimal amount,
	String deviationFromDate,
	String deviationToDate) {
}
