package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.Set;

/**
 * Domain summary of a person's ekonomiskt-bistånd footprint in Lifecare FC, distilled from aktualiseringar, beslut and
 * normberäkningar over the lookback window. Encapsulates FC's generated DTOs and date formats so callers reason in
 * domain terms.
 *
 * @param hasFootprint         the person is known to FC's EB process — at least one aktualisering, beslut or
 *                             normberäkning exists in the window (drives the "finns i LC?" existence gate)
 * @param decisionMonths       the set of year-months covered by a beslut within the window (used to check whether a
 *                             decision exists for a given month, and whether the current month is already decided)
 * @param latestDecisionPeriod the period (year-month) of the most recent beslut, or {@code null} when none
 * @param hasCalculation       a normberäkning exists within the window
 * @param hasCoApplicant       the most recent beslut included a co-applicant (used to infer the previous civilstånd)
 */
public record LifecareEbCaseSummary(
	boolean hasFootprint,
	Set<YearMonth> decisionMonths,
	YearMonth latestDecisionPeriod,
	boolean hasCalculation,
	boolean hasCoApplicant) {

	/** Empty summary — used when the person has no EB footprint, or as the best-effort fallback. */
	public static LifecareEbCaseSummary none() {
		return new LifecareEbCaseSummary(false, Set.of(), null, false, false);
	}
}
