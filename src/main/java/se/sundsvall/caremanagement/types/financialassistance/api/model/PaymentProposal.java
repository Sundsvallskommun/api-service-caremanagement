package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The payment proposal (utbetalningsförslag) for an errand — derived on every read from the calculation draft, the
 * applicant's previous Lifecare payments and the application, never stored. Computed when the DECISION section is
 * approved and on every GET; only the PAYMENT-section warnings it raises are persisted (and reconciled).
 */
@Schema(description = "The payment proposal (utbetalningsförslag) — derived data, recomputed on every read.")
public class PaymentProposal {

	@ArraySchema(schema = @Schema(implementation = ProposedPayment.class), arraySchema = @Schema(description = "The proposed payments — always exactly one entry from the service; the frontend may split it into several before registering"))
	private List<ProposedPayment> payments = new ArrayList<>();

	@ArraySchema(schema = @Schema(implementation = Payee.class),
		arraySchema = @Schema(description = "Every distinct payee seen on the applicant's Lifecare payments in the last 12 months — the dropdown alternatives. FamilyCare has no payee register, so this is the only source"))
	private List<Payee> payeeOptions = new ArrayList<>();

	@Schema(description = "Where the proposed payee came from: PREVIOUS_PAYMENT (the most recent Lifecare payment) or APPLICATION (the applicant stated a new account in the application, paymentSameAsPrevious=false). Null when no payee could be proposed",
		examples = "PREVIOUS_PAYMENT",
		allowableValues = {
			"PREVIOUS_PAYMENT", "APPLICATION"
		})
	private String payeeSource;

	@Schema(description = "The applicant's most recent Lifecare payment, or null when none was found (or Lifecare could not be read)", implementation = PreviousPayment.class)
	private PreviousPayment previousPayment;

	@Schema(description = "Why the proposal is incomplete (Swedish), e.g. no norm is known so no amount could be estimated. Null when the proposal is complete", examples = "Ingen norm kunde läsas från Lifecare – beloppet kunde inte beräknas.")
	private String explanation;

	@ArraySchema(schema = @Schema(implementation = Warning.class), arraySchema = @Schema(description = "The PAYMENT-section warnings this proposal raised (reconciled on every read)"))
	private List<Warning> warnings = new ArrayList<>();

	public static PaymentProposal create() {
		return new PaymentProposal();
	}

	public List<ProposedPayment> getPayments() {
		return payments;
	}

	public void setPayments(final List<ProposedPayment> payments) {
		this.payments = payments;
	}

	public PaymentProposal withPayments(final List<ProposedPayment> payments) {
		this.payments = payments;
		return this;
	}

	public List<Payee> getPayeeOptions() {
		return payeeOptions;
	}

	public void setPayeeOptions(final List<Payee> payeeOptions) {
		this.payeeOptions = payeeOptions;
	}

	public PaymentProposal withPayeeOptions(final List<Payee> payeeOptions) {
		this.payeeOptions = payeeOptions;
		return this;
	}

	public String getPayeeSource() {
		return payeeSource;
	}

	public void setPayeeSource(final String payeeSource) {
		this.payeeSource = payeeSource;
	}

	public PaymentProposal withPayeeSource(final String payeeSource) {
		this.payeeSource = payeeSource;
		return this;
	}

	public PreviousPayment getPreviousPayment() {
		return previousPayment;
	}

	public void setPreviousPayment(final PreviousPayment previousPayment) {
		this.previousPayment = previousPayment;
	}

	public PaymentProposal withPreviousPayment(final PreviousPayment previousPayment) {
		this.previousPayment = previousPayment;
		return this;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(final String explanation) {
		this.explanation = explanation;
	}

	public PaymentProposal withExplanation(final String explanation) {
		this.explanation = explanation;
		return this;
	}

	public List<Warning> getWarnings() {
		return warnings;
	}

	public void setWarnings(final List<Warning> warnings) {
		this.warnings = warnings;
	}

	public PaymentProposal withWarnings(final List<Warning> warnings) {
		this.warnings = warnings;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PaymentProposal that = (PaymentProposal) o;
		return Objects.equals(payments, that.payments) && Objects.equals(payeeOptions, that.payeeOptions) && Objects.equals(payeeSource, that.payeeSource)
			&& Objects.equals(previousPayment, that.previousPayment) && Objects.equals(explanation, that.explanation)
			&& Objects.equals(warnings, that.warnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(payments, payeeOptions, payeeSource, previousPayment, explanation, warnings);
	}

	@Override
	public String toString() {
		return "PaymentProposal{" +
			"payments=" + payments +
			", payeeOptions=" + payeeOptions +
			", payeeSource='" + payeeSource + '\'' +
			", previousPayment=" + previousPayment +
			", explanation='" + explanation + '\'' +
			", warnings=" + warnings +
			'}';
	}
}
