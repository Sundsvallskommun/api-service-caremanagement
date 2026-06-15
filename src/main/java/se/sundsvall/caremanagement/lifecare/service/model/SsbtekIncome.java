package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single normalised income read out of SSBTEK, expressed in SSBTEK's own terms (förmån / delförmån / beloppstyp) plus
 * the net amount, the date it is attributed to, and which household member it belongs to. This is the clean seam
 * between
 * the SSBTEK reader (which turns financial-aid's per-agency payloads into these) and
 * {@link se.sundsvall.caremanagement.lifecare.service.mapper.SsbtekToFcIncomeMapper}, which turns these into FC
 * normberäkning income rows. Period selection (kontroll-/jämförelseperiod transfer rules, see ssbtek-regelverk.txt) is
 * applied upstream — the mapper sums whatever incomes it is handed.
 *
 * @param forman     the SSBTEK förmån (e.g. "Bostadsbidrag", "Dagersättning") — the whitelist key
 * @param delforman  the SSBTEK delförmån, may be {@code null}
 * @param beloppstyp the SSBTEK beloppstyp, may be {@code null}
 * @param netAmount  the net amount (nettobelopp) to transfer
 * @param period     the date the income is attributed to (e.g. utbetalningsdatum / period start)
 * @param role       whether this income belongs to the applicant or the co-applicant
 */
public record SsbtekIncome(
	String forman,
	String delforman,
	String beloppstyp,
	BigDecimal netAmount,
	LocalDate period,
	ApplicantRole role) {
}
