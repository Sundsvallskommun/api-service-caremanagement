package se.sundsvall.caremanagement.conversation.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import se.sundsvall.caremanagement.conversation.api.validation.impl.ValidIdentifierConstraintValidator;

/**
 * Validates that a {@code X-Sent-By} header value is parseable into a dept44
 * {@link se.sundsvall.dept44.support.Identifier}
 * (i.e. {@code <value>; type=<type>}). A {@code null} value passes — pair the annotation with a required
 * {@code @RequestHeader} when the header is mandatory.
 */
@Target({
	ElementType.FIELD, ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidIdentifierConstraintValidator.class)
public @interface ValidIdentifier {

	String message() default "X-Sent-By must be in the format '<value>; type=<type>', e.g. 'joe001doe; type=adAccount'";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
