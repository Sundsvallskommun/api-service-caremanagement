package se.sundsvall.caremanagement.conversation.api.validation.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidIdentifierConstraintValidatorTest {

	private final ValidIdentifierConstraintValidator validator = new ValidIdentifierConstraintValidator();

	@Test
	void nullIsValid() {
		assertThat(validator.isValid(null, null)).isTrue();
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
