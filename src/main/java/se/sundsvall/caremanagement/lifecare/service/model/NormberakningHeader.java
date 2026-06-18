package se.sundsvall.caremanagement.lifecare.service.model;

import java.time.LocalDate;

/**
 * The header inputs of a normberäkning at commit — the chosen norm, the calculation date window (Från/Till/
 * Beräkningsdatum) and the custom household size (Gemensamma kostnader). Posted onto the FC
 * {@code PostCalculationBodyRequest}; any {@code null} field falls back to the assembler's application-month default or
 * is left for FC to derive.
 */
public record NormberakningHeader(
	Integer normId,
	LocalDate calculationFromDate,
	LocalDate calculationToDate,
	LocalDate calculationDate,
	Boolean hasCustomHouseholdSize,
	Integer householdSize) {
}
