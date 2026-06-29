package se.sundsvall.caremanagement.conversation.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.caremanagement.conversation.api.validation.ValidIdentifier;
import se.sundsvall.dept44.support.Identifier;

public class ValidIdentifierConstraintValidator implements ConstraintValidator<ValidIdentifier, String> {

	/**
	 * A {@code null} value passes (let {@code @RequestHeader} enforce presence); a non-null value is valid only when it
	 * parses into a dept44 {@link Identifier}.
	 */
	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return value == null || Identifier.parse(value) != null;
	}
}
