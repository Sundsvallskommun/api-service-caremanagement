package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.Set;

/**
 * Domain summary of a person's ekonomiskt-bistånd footprint in Lifecare FC, distilled from aktualiseringar, beslut and
 * normberäkningar. Encapsulates FC's generated DTOs and date formats so callers reason in domain terms.
 *
 * @param hasOpenCase                  the person is known to FC's EB process — an aktualisering or a beslut exists
 *                                     within
 *                                     the lookback window
 * @param hasDecisionForReferenceMonth a beslut covers the reference month (its from/to period spans it)
 * @param latestDecisionPeriod         the period (year-month) of the most recent beslut, or {@code null} when none
 * @param hasCalculation               a normberäkning exists within the lookback window
 * @param coApplicantPersonIds         co-applicant person ids found on the most recent beslut — empty when the latest
 *                                     decision was for a single applicant
 */
public record LifecareEbCaseSummary(
	boolean hasOpenCase,
	boolean hasDecisionForReferenceMonth,
	YearMonth latestDecisionPeriod,
	boolean hasCalculation,
	Set<String> coApplicantPersonIds) {

	/** Empty summary — used when the person has no EB footprint, or as the best-effort fallback. */
	public static LifecareEbCaseSummary none() {
		return new LifecareEbCaseSummary(false, false, null, false, Set.of());
	}
}
