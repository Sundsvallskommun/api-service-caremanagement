package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * The caseworker's decision on the application, as entered in the Draken decision section. Recorded on the errand as a
 * {@code PAYMENT} decision; Draken's BFF writes it into Lifecare and reports back through {@code .../lifecare-result}.
 */
@Schema(description = "The caseworker's decision on the application — outcome, period, amount and what is communicated to the applicant.")
public class FinalizeDecision {

	@Schema(
		description = "Decision outcome code. BIFALL/DELAVSLAG grant an amount (and require payments); AVSLAG grants nothing. DELAVSLAG is labelled \"Delvis bifall\" in the dropdown (see the errand type's decision options). AVVISNING was dropped 2026-09-21 and is no longer accepted.",
		examples = "BIFALL",
		allowableValues = {
			"BIFALL", "DELAVSLAG", "AVSLAG"
		},
		requiredMode = Schema.RequiredMode.REQUIRED)
	@OneOf({
		"BIFALL", "DELAVSLAG", "AVSLAG"
	})
	private String outcome;

	@Schema(description = "Internal motivation for the decision — stored as the decision's description, not shown to the applicant", examples = "Inkomster enligt SSBTEK, hyra styrkt")
	@Size(max = 4096)
	private String reason;

	@Schema(description = """
		The co-applicant's orsak, when the household has a co-applicant — picked from the same reasonOptions as reason. \
		Stored on the decision as coApplicantReason.""", examples = "Beviljad")
	@Size(max = 255)
	private String coApplicantReason;

	@Schema(description = "Start of the period the decision covers (the month applied for)", examples = "2026-06-01")
	@DateTimeFormat(iso = DATE)
	private LocalDate periodFrom;

	@Schema(description = "End of the period the decision covers", examples = "2026-06-30")
	@DateTimeFormat(iso = DATE)
	private LocalDate periodTo;

	@Schema(description = "The granted amount in SEK. Required when the outcome carries an amount (BIFALL/DELAVSLAG); ignored and recorded as 0 otherwise.", examples = "7900.00")
	@PositiveOrZero
	private BigDecimal amount;

	@Schema(description = "The underrättelse — the free-text decision message communicated to the applicant on the decision letter", examples = "Du beviljas ekonomiskt bistånd för juni 2026 enligt riksnorm.")
	@Size(max = 8192)
	private String decisionMessage;

	public static FinalizeDecision create() {
		return new FinalizeDecision();
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public FinalizeDecision withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public FinalizeDecision withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	public String getCoApplicantReason() {
		return coApplicantReason;
	}

	public void setCoApplicantReason(final String coApplicantReason) {
		this.coApplicantReason = coApplicantReason;
	}

	public FinalizeDecision withCoApplicantReason(final String coApplicantReason) {
		this.coApplicantReason = coApplicantReason;
		return this;
	}

	public LocalDate getPeriodFrom() {
		return periodFrom;
	}

	public void setPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
	}

	public FinalizeDecision withPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public LocalDate getPeriodTo() {
		return periodTo;
	}

	public void setPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
	}

	public FinalizeDecision withPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public FinalizeDecision withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getDecisionMessage() {
		return decisionMessage;
	}

	public void setDecisionMessage(final String decisionMessage) {
		this.decisionMessage = decisionMessage;
	}

	public FinalizeDecision withDecisionMessage(final String decisionMessage) {
		this.decisionMessage = decisionMessage;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinalizeDecision that = (FinalizeDecision) o;
		return Objects.equals(outcome, that.outcome) && Objects.equals(reason, that.reason) && Objects.equals(coApplicantReason, that.coApplicantReason)
			&& Objects.equals(periodFrom, that.periodFrom)
			&& Objects.equals(periodTo, that.periodTo) && Objects.equals(amount, that.amount) && Objects.equals(decisionMessage, that.decisionMessage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outcome, reason, coApplicantReason, periodFrom, periodTo, amount, decisionMessage);
	}

	@Override
	public String toString() {
		return "FinalizeDecision{" +
			"outcome='" + outcome + '\'' +
			", reason='" + reason + '\'' +
			", coApplicantReason='" + coApplicantReason + '\'' +
			", periodFrom=" + periodFrom +
			", periodTo=" + periodTo +
			", amount=" + amount +
			", decisionMessage='" + decisionMessage + '\'' +
			'}';
	}
}
