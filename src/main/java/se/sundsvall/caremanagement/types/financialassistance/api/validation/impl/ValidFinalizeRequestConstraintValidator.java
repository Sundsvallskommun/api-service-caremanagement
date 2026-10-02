package se.sundsvall.caremanagement.types.financialassistance.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.validation.ValidFinalizeRequest;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.OUTCOMES;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;

public class ValidFinalizeRequestConstraintValidator implements ConstraintValidator<ValidFinalizeRequest, FinalizeRequest> {

	static final String NODE_AMOUNT = "decision.amount";
	static final String NODE_PERIOD_TO = "decision.periodTo";

	static final String ERROR_AMOUNT_REQUIRED = "must be given when the outcome carries an amount (BIFALL/DELAVSLAG)";
	static final String ERROR_PERIOD_ORDER = "must not be before periodFrom";

	@Override
	public boolean isValid(final FinalizeRequest request, final ConstraintValidatorContext context) {
		// The decision itself is @NotNull/@OneOf-validated on its own; without a recognised outcome there is no rule to
		// apply here, and reporting one would only duplicate (or contradict) the field violation.
		final var decision = ofNullable(request).map(FinalizeRequest::getDecision).orElse(null);
		if (decision == null || decision.getOutcome() == null || !OUTCOMES.contains(decision.getOutcome())) {
			return true;
		}

		var isValid = true;

		if (outcomeCarriesAmount(decision.getOutcome()) && decision.getAmount() == null) {
			addViolation(context, NODE_AMOUNT, ERROR_AMOUNT_REQUIRED);
			isValid = false;
		}

		if (periodEndsBeforeStart(decision)) {
			addViolation(context, NODE_PERIOD_TO, ERROR_PERIOD_ORDER);
			isValid = false;
		}

		return isValid;
	}

	private static boolean periodEndsBeforeStart(final FinalizeDecision decision) {
		return decision.getPeriodFrom() != null && decision.getPeriodTo() != null && decision.getPeriodTo().isBefore(decision.getPeriodFrom());
	}

	private static void addViolation(final ConstraintValidatorContext context, final String node, final String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message)
			.addPropertyNode(node)
			.addConstraintViolation();
	}
}
