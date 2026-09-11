package se.sundsvall.caremanagement.lifecare.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single normalised income read out of SSBTEK, expressed in SSBTEK's own terms (benefit / sub-benefit / amountType)
 * plus the net amount, the date it is attributed to, and which household member it belongs to. It backs {@link
 * ClassifiedIncome}: the operaton regelverk classifies these (raw list + thresholds + period selection) and
 * caremanagement maps the result onto FamilyCare calculation income rows via {@link
 * se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFamilyCareMapper}.
 *
 * <p>
 * Deserialised from the {@code classifiedIncomes[].income} JSON the operaton {@code evaluate-income-regelverk}
 * worker produces. As with {@link ClassifiedIncome}, the JSON keys stay Swedish (operaton's {@code SsbtekIncome}
 * serialises its record components {@code forman}/{@code delforman}/{@code beloppstyp} verbatim), mapped onto the
 * English record components via {@link JsonProperty}. {@code netAmount}/{@code period}/{@code role} already share the
 * wire key.
 *
 * @param benefit    the SSBTEK benefit ({@code forman}, e.g. "Bostadsbidrag", "Dagersättning") — the whitelist key
 * @param subBenefit the SSBTEK sub-benefit ({@code delforman}), may be {@code null}
 * @param amountType the SSBTEK amountType ({@code beloppstyp}), may be {@code null}
 * @param netAmount  the net amount (nettobelopp) to transfer
 * @param period     the date the income is attributed to (e.g. payment date / period start)
 * @param periodFrom the first day of the period the payment covers ({@code periodFran}), may be {@code null}
 * @param periodTo   the last day of the period the payment covers ({@code periodTill}), may be {@code null}
 * @param days       the number of days the payment is for ({@code dagar}), may be {@code null}
 * @param role       whether this income belongs to the applicant or the co-applicant
 */
public record SsbtekIncome(
	@JsonProperty("forman") String benefit,
	@JsonProperty("delforman") String subBenefit,
	@JsonProperty("beloppstyp") String amountType,
	BigDecimal netAmount,
	LocalDate period,
	@JsonProperty("periodFran") LocalDate periodFrom,
	@JsonProperty("periodTill") LocalDate periodTo,
	@JsonProperty("dagar") Integer days,
	ApplicantRole role) {

	/** The payment-date-only shape, for tests and callers with no period or day information. */
	public SsbtekIncome(final String benefit, final String subBenefit, final String amountType,
		final BigDecimal netAmount, final LocalDate period, final ApplicantRole role) {
		this(benefit, subBenefit, amountType, netAmount, period, null, null, null, role);
	}
}
