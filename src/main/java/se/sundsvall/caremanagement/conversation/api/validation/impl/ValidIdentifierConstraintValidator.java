package se.sundsvall.caremanagement.conversation.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.caremanagement.conversation.api.validation.ValidIdentifier;
import se.sundsvall.dept44.support.Identifier;

public class ValidIdentifierConstraintValidator implements ConstraintValidator<ValidIdentifier, String> {

	private boolean nullable;

	@Override
	public void initialize(final ValidIdentifier constraintAnnotation) {
		this.nullable = constraintAnnotation.nullable();
	}

	/**
	 * A {@code null} value is valid only when the annotation is {@code nullable}; a non-null value is valid only when it
	 * parses into a dept44 {@link Identifier}.
	 */
	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null) {
			return nullable;
		}
		return Identifier.parse(value) != null;
	}
}
