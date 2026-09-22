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

class PaymentLifecareResultTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(PaymentLifecareResult.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = PaymentLifecareResult.create()
			.withOutcome("REGISTERED")
			.withLifecarePaymentId("4")
			.withDetail("Betalningsmottagaren saknas i Lifecare");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getOutcome()).isEqualTo("REGISTERED");
		assertThat(result.getLifecarePaymentId()).isEqualTo("4");
		assertThat(result.getDetail()).isEqualTo("Betalningsmottagaren saknas i Lifecare");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PaymentLifecareResult.create()).hasAllNullFieldsOrProperties();
	}
}
