package se.sundsvall.caremanagement.conversation.api.validation.impl;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.conversation.api.validation.ValidIdentifier;

import static org.assertj.core.api.Assertions.assertThat;

class ValidIdentifierConstraintValidatorTest {

	@ValidIdentifier
	private String defaultField;

	@ValidIdentifier(nullable = true)
	private String nullableField;

	private final ValidIdentifierConstraintValidator validator = new ValidIdentifierConstraintValidator();

	private ValidIdentifierConstraintValidator initializedFrom(final String fieldName) throws NoSuchFieldException {
		final var annotation = getClass().getDeclaredField(fieldName).getAnnotation(ValidIdentifier.class);
		final var initialized = new ValidIdentifierConstraintValidator();
		initialized.initialize(annotation);
		return initialized;
	}

	@Test
	void nullIsInvalidByDefault() throws NoSuchFieldException {
		assertThat(initializedFrom("defaultField").isValid(null, null)).isFalse();
	}

	@Test
	void nullIsValidWhenNullable() throws NoSuchFieldException {
		assertThat(initializedFrom("nullableField").isValid(null, null)).isTrue();
	}

	@Test
	void adAccountIsValid() {
		assertThat(validator.isValid("joe001doe; type=adAccount", null)).isTrue();
	}

	@Test
	void partyIdIsValid() {
		assertThat(validator.isValid("f47ac10b-58cc-4372-a567-0e02b2c3d479; type=partyId", null)).isTrue();
	}

	@Test
	void unparseableIsInvalid() {
		assertThat(validator.isValid("garbage-without-a-type", null)).isFalse();
	}

	@Test
	void blankIsInvalid() {
		assertThat(validator.isValid("   ", null)).isFalse();
	}
}
