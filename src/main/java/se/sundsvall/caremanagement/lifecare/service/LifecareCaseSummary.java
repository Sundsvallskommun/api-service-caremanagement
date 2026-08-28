package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.Set;

/**
 * Domain summary of a person's financial-assistance footprint in Lifecare FamilyCare, distilled from actualisations,
 * decision and calculations over the lookback window. Encapsulates FamilyCare's generated DTOs and date formats so
 * callers reason in domain terms.
 *
 * @param hasFootprint         the person is known to FamilyCare's financial assistance process — at least one
 *                             actualisation,
 *                             decision or calculation exists in the window (drives the "finns i LC?" existence gate)
 * @param hasOpenCase          whether an actualisation with an open status exists — {@code TRUE} when one does,
 *                             {@code FALSE} when statuses were readable but none is open, and {@code null} when no
 *                             actualisation carried a readable status (unknown). Reported alongside
 *                             {@code hasFootprint} while the FamilyCare status vocabulary is verified against
 *                             production data; see {@code financial-assistance.eligibility.require-open-case}
 * @param decisionMonths       the set of year-months covered by a decision within the window (used to check whether a
 *                             decision exists for a given month, and whether the current month is already decided)
 * @param latestDecisionPeriod the period (year-month) of the most recent decision, or {@code null} when none
 * @param hasCalculation       a calculation exists within the window
 * @param hasCoApplicant       the most recent decision included a co-applicant (used to infer the previous marital
 *                             status)
 */
public record LifecareCaseSummary(
	boolean hasFootprint,
	Boolean hasOpenCase,
	Set<YearMonth> decisionMonths,
	YearMonth latestDecisionPeriod,
	boolean hasCalculation,
	boolean hasCoApplicant) {

	/** Empty summary — used when the person has no financial assistance footprint, or as the best-effort fallback. */
	public static LifecareCaseSummary none() {
		return new LifecareCaseSummary(false, null, Set.of(), null, false, false);
	}
}
