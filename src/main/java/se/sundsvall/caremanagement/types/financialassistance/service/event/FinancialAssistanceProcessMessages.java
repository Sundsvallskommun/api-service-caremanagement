package se.sundsvall.caremanagement.types.financialassistance.service.event;

/**
 * The BPMN message and variable names the financial assistance renewal process ({@code rakel-ekonomiskt-bistand},
 * see {@link FinancialAssistanceProcessStarter#PROCESS_DEFINITION_NAME}) listens for while it waits at the
 * caseworker-decision gateway. They mirror the deployed model — the event-based gateway catches
 * {@code PaymentDecisionReceived} and routes on {@code ${paymentDecision == "APPROVED"}}, anything else taking the
 * Avslagen path — so a rename here without a model change (or vice versa) leaves the process waiting forever.
 */
public final class FinancialAssistanceProcessMessages {

	private FinancialAssistanceProcessMessages() {}

	/** The message correlated (businessKey = errandId) when the caseworker presses "Besluta och utbetala". */
	public static final String MESSAGE_PAYMENT_DECISION_RECEIVED = "PaymentDecisionReceived";

	/** The process variable the gateway routes on. */
	public static final String VARIABLE_PAYMENT_DECISION = "paymentDecision";

	/** The variable value for a granting decision — GRANTED, poll the payment. */
	public static final String PAYMENT_DECISION_APPROVED = "APPROVED";

	/** The variable value for a non-granting decision — REJECTED, close. */
	public static final String PAYMENT_DECISION_REJECTED = "REJECTED";
}
