package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.errandtypes.api.model.DecisionOption;

/**
 * The decision proposal (beslutsförslag) for an errand — derived on every read from the calculation draft and the
 * applicant's previous Lifecare decision, never stored. Computed when the CALCULATION section is approved and on every
 * GET; only the DECISION-section warnings it raises are persisted (and reconciled).
 *
 * <p>
 * Outcome rule: {@code estimatedAmount <= 0} → AVSLAG; {@code > 0} and every expense fully approved → BIFALL;
 * {@code > 0} and any expense approved below its applied amount → DELAVSLAG. Once the normberäkning is saved in
 * Lifecare, the amount is that calculation's underskott ({@code -balance}); before that it is estimated as
 * {@code normSum + expenseSum + specialExpenseSum - incomeSum}, where the norm comes from the applicant's most recent
 * Lifecare calculation before the application month (the draft carries no norm). {@code amountBasis} says which.
 * </p>
 */
@Schema(description = "The decision proposal (beslutsförslag) — derived data, recomputed on every read.")
public class DecisionProposal {

	@Schema(
		description = "The proposed decision outcome. Rule: estimatedAmount <= 0 → AVSLAG; estimatedAmount > 0 and every expense fully approved → BIFALL; estimatedAmount > 0 and any expense approved below the applied amount → DELAVSLAG. Null when no amount could be estimated (see explanation)",
		examples = "BIFALL",
		allowableValues = {
			"BIFALL", "DELAVSLAG", "AVSLAG"
		})
	private String outcome;

	@ArraySchema(schema = @Schema(implementation = DecisionOption.class), arraySchema = @Schema(description = "Every outcome the caseworker can pick instead — the errand type's decision catalogue"))
	private List<DecisionOption> outcomeOptions = new ArrayList<>();

	@Schema(description = "The proposed decision period start — the calculation's period", examples = "2026-06-01")
	private LocalDate periodFrom;

	@Schema(description = "The proposed decision period end — the calculation's period", examples = "2026-06-30")
	private LocalDate periodTo;

	@Schema(description = "The calculation's application month (YYYY-MM)", examples = "2026-06")
	private String concernedMonth;

	@Schema(
		description = "The bistånd the outcome is decided on; positive is an underskott, zero or negative an överskott. When the normberäkning is saved in Lifecare (lifecareCalculationId) this is its result as Lifecare computed it (-balance) — amountBasis LIFECARE_CALCULATION. Before that, or when the saved calculation cannot be read, it is estimated: normSum + expenseSum + specialExpenseSum − incomeSum, all from the calculation draft except the norm, which is taken from the applicant's most recent Lifecare calculation before the application month — amountBasis ESTIMATE. Null when no previous norm is known (see explanation)",
		examples = "4250")
	private BigDecimal estimatedAmount;

	@Schema(
		description = "Where estimatedAmount (and thereby outcome) comes from: LIFECARE_CALCULATION = the normberäkning saved in Lifecare, ESTIMATE = the draft plus the previous calculation's norm. Null when there is no amount",
		examples = "LIFECARE_CALCULATION",
		allowableValues = {
			"LIFECARE_CALCULATION", "ESTIMATE"
		})
	private String amountBasis;

	@Schema(description = "The norm sum: the saved Lifecare calculation's norm (LIFECARE_CALCULATION), or the previous Lifecare calculation's norm the estimate is based on (ESTIMATE). Null when unknown", examples = "6200")
	private BigDecimal normSum;

	@Schema(description = "The draft's income sum (effective amounts)", examples = "3000")
	private BigDecimal incomeSum;

	@Schema(description = "The draft's expense sum (effective = approved amounts)", examples = "800")
	private BigDecimal expenseSum;

	@Schema(description = "The draft's special-expense sum (effective = approved amounts)", examples = "250")
	private BigDecimal specialExpenseSum;

	@Schema(description = "Why the proposal is incomplete (Swedish) — e.g. no previous norm could be read so no amount/outcome was proposed. Null when the proposal is complete",
		examples = "Ingen norm kunde läsas från Lifecare – beloppet kunde inte beräknas.")
	private String explanation;

	@Schema(description = "The proposed orsak: the previous Lifecare decision's reason, or null when there is none", examples = "Arbetslös, ingen ersättning/stöd")
	private String reason;

	@ArraySchema(schema = @Schema(implementation = String.class),
		arraySchema = @Schema(
			description = "Every orsak the caseworker can pick instead — Lifecare's orsak-catalogue (försörjningshinder) in Lifecare's order, plus the previous decision's reason(s) when those are not in the catalogue (FamilyCare exposes no reason catalogue over the API). The same catalogue applies to the applicant and the co-applicant"))
	private List<String> reasonOptions = new ArrayList<>();

	@Schema(description = "The proposed orsak for the co-applicant (medsökande): the previous Lifecare decision's co-applicant reason, or null when there is none. Picked from the same reasonOptions catalogue as the applicant's",
		examples = "Sjukskriven m läkarintyg, otillräcklig sjukpenning")
	private String coApplicantReason;

	@Schema(description = "The applicant's most recent Lifecare decision, or null when none was found (or Lifecare could not be read)", implementation = PreviousDecision.class)
	private PreviousDecision previousDecision;

	@ArraySchema(schema = @Schema(implementation = Warning.class), arraySchema = @Schema(description = "The DECISION-section warnings this proposal raised (reconciled on every read)"))
	private List<Warning> warnings = new ArrayList<>();

	public static DecisionProposal create() {
		return new DecisionProposal();
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public DecisionProposal withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public List<DecisionOption> getOutcomeOptions() {
		return outcomeOptions;
	}

	public void setOutcomeOptions(final List<DecisionOption> outcomeOptions) {
		this.outcomeOptions = outcomeOptions;
	}

	public DecisionProposal withOutcomeOptions(final List<DecisionOption> outcomeOptions) {
		this.outcomeOptions = outcomeOptions;
		return this;
	}

	public LocalDate getPeriodFrom() {
		return periodFrom;
	}

	public void setPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
	}

	public DecisionProposal withPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public LocalDate getPeriodTo() {
		return periodTo;
	}

	public void setPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
	}

	public DecisionProposal withPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
		return this;
	}

	public String getConcernedMonth() {
		return concernedMonth;
	}

	public void setConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
	}

	public DecisionProposal withConcernedMonth(final String concernedMonth) {
		this.concernedMonth = concernedMonth;
		return this;
	}

	public BigDecimal getEstimatedAmount() {
		return estimatedAmount;
	}

	public void setEstimatedAmount(final BigDecimal estimatedAmount) {
		this.estimatedAmount = estimatedAmount;
	}

	public DecisionProposal withEstimatedAmount(final BigDecimal estimatedAmount) {
		this.estimatedAmount = estimatedAmount;
		return this;
	}

	public String getAmountBasis() {
		return amountBasis;
	}

	public void setAmountBasis(final String amountBasis) {
		this.amountBasis = amountBasis;
	}

	public DecisionProposal withAmountBasis(final String amountBasis) {
		this.amountBasis = amountBasis;
		return this;
	}

	public BigDecimal getNormSum() {
		return normSum;
	}

	public void setNormSum(final BigDecimal normSum) {
		this.normSum = normSum;
	}

	public DecisionProposal withNormSum(final BigDecimal normSum) {
		this.normSum = normSum;
		return this;
	}

	public BigDecimal getIncomeSum() {
		return incomeSum;
	}

	public void setIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
	}

	public DecisionProposal withIncomeSum(final BigDecimal incomeSum) {
		this.incomeSum = incomeSum;
		return this;
	}

	public BigDecimal getExpenseSum() {
		return expenseSum;
	}

	public void setExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
	}

	public DecisionProposal withExpenseSum(final BigDecimal expenseSum) {
		this.expenseSum = expenseSum;
		return this;
	}

	public BigDecimal getSpecialExpenseSum() {
		return specialExpenseSum;
	}

	public void setSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
	}

	public DecisionProposal withSpecialExpenseSum(final BigDecimal specialExpenseSum) {
		this.specialExpenseSum = specialExpenseSum;
		return this;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(final String explanation) {
		this.explanation = explanation;
	}

	public DecisionProposal withExplanation(final String explanation) {
		this.explanation = explanation;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public DecisionProposal withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	public List<String> getReasonOptions() {
		return reasonOptions;
	}

	public void setReasonOptions(final List<String> reasonOptions) {
		this.reasonOptions = reasonOptions;
	}

	public DecisionProposal withReasonOptions(final List<String> reasonOptions) {
		this.reasonOptions = reasonOptions;
		return this;
	}

	public String getCoApplicantReason() {
		return coApplicantReason;
	}

	public void setCoApplicantReason(final String coApplicantReason) {
		this.coApplicantReason = coApplicantReason;
	}

	public DecisionProposal withCoApplicantReason(final String coApplicantReason) {
		this.coApplicantReason = coApplicantReason;
		return this;
	}

	public PreviousDecision getPreviousDecision() {
		return previousDecision;
	}

	public void setPreviousDecision(final PreviousDecision previousDecision) {
		this.previousDecision = previousDecision;
	}

	public DecisionProposal withPreviousDecision(final PreviousDecision previousDecision) {
		this.previousDecision = previousDecision;
		return this;
	}

	public List<Warning> getWarnings() {
		return warnings;
	}

	public void setWarnings(final List<Warning> warnings) {
		this.warnings = warnings;
	}

	public DecisionProposal withWarnings(final List<Warning> warnings) {
		this.warnings = warnings;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final DecisionProposal that = (DecisionProposal) o;
		return Objects.equals(outcome, that.outcome) && Objects.equals(outcomeOptions, that.outcomeOptions) && Objects.equals(periodFrom, that.periodFrom)
			&& Objects.equals(periodTo, that.periodTo) && Objects.equals(concernedMonth, that.concernedMonth)
			&& Objects.equals(estimatedAmount, that.estimatedAmount) && Objects.equals(amountBasis, that.amountBasis) && Objects.equals(normSum, that.normSum) && Objects.equals(incomeSum, that.incomeSum)
			&& Objects.equals(expenseSum, that.expenseSum) && Objects.equals(specialExpenseSum, that.specialExpenseSum)
			&& Objects.equals(explanation, that.explanation) && Objects.equals(reason, that.reason) && Objects.equals(reasonOptions, that.reasonOptions)
			&& Objects.equals(coApplicantReason, that.coApplicantReason)
			&& Objects.equals(previousDecision, that.previousDecision) && Objects.equals(warnings, that.warnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outcome, outcomeOptions, periodFrom, periodTo, concernedMonth, estimatedAmount, amountBasis, normSum, incomeSum, expenseSum, specialExpenseSum, explanation, reason, reasonOptions,
			coApplicantReason, previousDecision, warnings);
	}

	@Override
	public String toString() {
		return "DecisionProposal{" +
			"outcome='" + outcome + '\'' +
			", outcomeOptions=" + outcomeOptions +
			", periodFrom=" + periodFrom +
			", periodTo=" + periodTo +
			", concernedMonth='" + concernedMonth + '\'' +
			", estimatedAmount=" + estimatedAmount +
			", amountBasis='" + amountBasis + '\'' +
			", normSum=" + normSum +
			", incomeSum=" + incomeSum +
			", expenseSum=" + expenseSum +
			", specialExpenseSum=" + specialExpenseSum +
			", explanation='" + explanation + '\'' +
			", reason='" + reason + '\'' +
			", reasonOptions=" + reasonOptions +
			", coApplicantReason='" + coApplicantReason + '\'' +
			", previousDecision=" + previousDecision +
			", warnings=" + warnings +
			'}';
	}
}
