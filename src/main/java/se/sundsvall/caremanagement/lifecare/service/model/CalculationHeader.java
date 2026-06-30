package se.sundsvall.caremanagement.lifecare.service.model;

import java.time.LocalDate;

/**
 * The header inputs of a calculation at commit — the chosen norm, the calculation date window (from/to/
 * calculation date) and the custom household size (common costs). Posted onto the FC
 * {@code PostCalculationBodyRequest}; any {@code null} field falls back to the assembler's application-month default or
 * is left for FC to derive.
 */
public record CalculationHeader(
	Integer normId,
	LocalDate calculationFromDate,
	LocalDate calculationToDate,
	LocalDate calculationDate,
	Boolean hasCustomHouseholdSize,
	Integer householdSize) {
}
