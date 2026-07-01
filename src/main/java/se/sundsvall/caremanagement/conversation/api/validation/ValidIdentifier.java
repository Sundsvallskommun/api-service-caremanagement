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
 * {@link se.sundsvall.dept44.support.Identifier} (i.e. {@code <value>; type=<type>}). A {@code null} value is rejected
 * by default; set {@link #nullable()} to {@code true} to accept {@code null} (e.g. for a genuinely optional header).
 */
@Target({
	ElementType.FIELD, ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidIdentifierConstraintValidator.class)
public @interface ValidIdentifier {

	String message() default "X-Sent-By must be in the format '<value>; type=<type>', e.g. 'joe001doe; type=adAccount'";

	/**
	 * Controls whether a {@code null} value is accepted. When {@code false} (default) a {@code null} value is rejected;
	 * when {@code true} it passes, leaving presence to be enforced elsewhere (e.g. a required {@code @RequestHeader}).
	 *
	 * @return {@code true} if {@code null} is accepted as valid, {@code false} otherwise.
	 */
	boolean nullable() default false;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
