package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single normalised income read out of SSBTEK, expressed in SSBTEK's own terms (benefit / sub-benefit / amountType)
 * plus
 * the net amount, the date it is attributed to, and which household member it belongs to. It backs
 * {@link ClassifiedIncome}: the operaton regelverk classifies these (raw list + thresholds + period selection) and
 * caremanagement maps the result onto FC calculation income rows via
 * {@link se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFcMapper}.
 *
 * @param benefit    the SSBTEK benefit (e.g. "Bostadsbidrag", "Dagersättning") — the whitelist key
 * @param subBenefit the SSBTEK sub-benefit, may be {@code null}
 * @param amountType the SSBTEK amountType, may be {@code null}
 * @param netAmount  the net amount (nettobelopp) to transfer
 * @param period     the date the income is attributed to (e.g. payment date / period start)
 * @param role       whether this income belongs to the applicant or the co-applicant
 */
public record SsbtekIncome(
	String benefit,
	String subBenefit,
	String amountType,
	BigDecimal netAmount,
	LocalDate period,
	ApplicantRole role) {
}
