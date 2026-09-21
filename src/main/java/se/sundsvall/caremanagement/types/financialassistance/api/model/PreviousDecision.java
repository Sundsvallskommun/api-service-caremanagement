package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * The applicant's most recent Lifecare decision — the baseline the decision proposal takes its reason from and checks
 * for förskott på förmån. Type and reason are Lifecare's own free-text values (no codes exist); dates are raw strings.
 */
@Schema(description = "The applicant's most recent Lifecare decision.")
public class PreviousDecision {

	@Schema(description = "The Lifecare decision type (free text as Lifecare names it)", examples = "Bifall")
	private String type;

	@Schema(description = "The Lifecare decision reason / orsak (free text)", examples = "Arbetslös, ingen ersättning/stöd")
	private String reason;

	@Schema(description = "The co-applicant (medsökande) the decision also concerned, as Lifecare names them. Null when the decision had none", examples = "Astrid Testsson")
	private String coApplicant;

	@Schema(description = "The co-applicant's own reason / orsak on the decision (free text). Null when the decision had no co-applicant", examples = "Sjukskriven m läkarintyg, otillräcklig sjukpenning")
	private String coApplicantReason;

	@Schema(description = "The decision period start (raw Lifecare string)", examples = "2026-05-01")
	private String periodFrom;

	@Schema(description = "The decision period end (raw Lifecare string)", examples = "2026-05-31")
	private String periodTo;

	@Schema(description = "The decided amount", examples = "8500")
	private BigDecimal amount;

	@Schema(description = "The decision date (raw Lifecare string)", examples = "2026-04-28")
	private String date;

	public static PreviousDecision create() {
		return new PreviousDecision();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public PreviousDecision withType(final String type) {
		this.type = type;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public PreviousDecision withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	public String getCoApplicant() {
		return coApplicant;
	}

	public void setCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
	}

	public PreviousDecision withCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
		return this;
	}

	public String getCoApplicantReason() {
		return coApplicantReason;
	}

	public void setCoApplicantReason(final String coApplicantReason) {
		this.coApplicantReason = coApplicantReason;
	}

	public PreviousDecision withCoApplicantReason(final String coApplicantReason) {
		this.coApplicantReason = coApplicantReason;
		return this;
	}

	public String getPeriodFrom() {
		return periodFrom;
	}

	public void setPeriodFrom(final String periodFrom) {
		this.periodFrom = periodFrom;
	}

	public PreviousDecision withPeriodFrom(final String periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public String getPeriodTo() {
		return periodTo;
	}

	public void setPeriodTo(final String periodTo) {
		this.periodTo = periodTo;
	}

	public PreviousDecision withPeriodTo(final String periodTo) {
		this.periodTo = periodTo;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public PreviousDecision withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public PreviousDecision withDate(final String date) {
		this.date = date;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PreviousDecision that = (PreviousDecision) o;
		return Objects.equals(type, that.type) && Objects.equals(reason, that.reason) && Objects.equals(coApplicant, that.coApplicant)
			&& Objects.equals(coApplicantReason, that.coApplicantReason) && Objects.equals(periodFrom, that.periodFrom)
			&& Objects.equals(periodTo, that.periodTo) && Objects.equals(amount, that.amount) && Objects.equals(date, that.date);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, reason, coApplicant, coApplicantReason, periodFrom, periodTo, amount, date);
	}

	@Override
	public String toString() {
		return "PreviousDecision{" +
			"type='" + type + '\'' +
			", reason='" + reason + '\'' +
			", coApplicant='" + coApplicant + '\'' +
			", coApplicantReason='" + coApplicantReason + '\'' +
			", periodFrom='" + periodFrom + '\'' +
			", periodTo='" + periodTo + '\'' +
			", amount=" + amount +
			", date='" + date + '\'' +
			'}';
	}
}
