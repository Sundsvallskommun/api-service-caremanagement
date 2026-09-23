package se.sundsvall.caremanagement.lifecare.service.model;

import java.time.LocalDate;

/**
 * The header inputs of a calculation at commit — the chosen norm, the calculation date window (from/to/ calculation
 * date) and the custom household size (common costs). Posted onto the FamilyCare {@code PostCalculationBodyRequest};
 * any {@code null} field falls back to the assembler's application-month default or is left for FamilyCare to derive.
 *
 * <p>
 * {@code serviceId} is the errand's Lifecare EB insats — the one the calculation is created under. A calculation
 * belongs to an insats <em>or</em> an utredning, never both (FamilyCare: "Cannot contain both investigation and
 * service"), and ekonomiskt bistånd calculates under the insats.
 * </p>
 */
public record CalculationHeader(
	Integer normId,
	LocalDate calculationFromDate,
	LocalDate calculationToDate,
	LocalDate calculationDate,
	Boolean hasCustomHouseholdSize,
	Integer householdSize,
	Integer serviceId) {
}
