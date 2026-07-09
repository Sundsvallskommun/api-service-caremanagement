package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class PaymentStatusResponseTest {

	@Test
	void testBean() {
		assertThat(PaymentStatusResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var response = PaymentStatusResponse.create()
			.withEffectuated(true)
			.withPaymentDate("2026-05-27");

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
	}

	@Test
	void testCreateReturnsBlankInstance() {
		assertThat(PaymentStatusResponse.create()).hasAllNullFieldsOrProperties();
	}
}
