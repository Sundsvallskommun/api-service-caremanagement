package se.sundsvall.caremanagement.types.financialassistance.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The cross-field rules of a finalize request: a granting outcome (BIFALL/DELAVSLAG) needs an amount and at least one
 * payment, a non-granting one (AVSLAG) must carry no payments, and the decision period must not end before it
 * starts. Each broken rule is reported as its own violation on the field it concerns.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidFinalizeRequestConstraintValidator.class)
public @interface ValidFinalizeRequest {

	String message() default "invalid finalize request";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
