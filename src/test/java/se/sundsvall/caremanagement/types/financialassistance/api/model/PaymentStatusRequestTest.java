package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PaymentStatusRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(PaymentStatusRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var request = PaymentStatusRequest.create()
			.withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479")
			.withApplicationMonth("2026-06");

		assertThat(request.getApplicant()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PaymentStatusRequest.create()).hasAllNullFieldsOrProperties();
	}
}
