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

class PayeeRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(PayeeRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = PayeeRequest.create()
			.withName("Sundsvalls Hyresbostäder AB")
			.withPaymentMethod("Bankgiro via Plusgiro")
			.withClearing("6000")
			.withAccountNumber("5555-6666");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getName()).isEqualTo("Sundsvalls Hyresbostäder AB");
		assertThat(result.getPaymentMethod()).isEqualTo("Bankgiro via Plusgiro");
		assertThat(result.getClearing()).isEqualTo("6000");
		assertThat(result.getAccountNumber()).isEqualTo("5555-6666");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PayeeRequest.create()).hasAllNullFieldsOrProperties();
	}
}
