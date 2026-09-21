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

class PayeeLifecareResultTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(PayeeLifecareResult.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = PayeeLifecareResult.create()
			.withOutcome("ADDED")
			.withLifecarePayeeId("44213")
			.withDetail("Kontonummer har fel format");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getOutcome()).isEqualTo("ADDED");
		assertThat(result.getLifecarePayeeId()).isEqualTo("44213");
		assertThat(result.getDetail()).isEqualTo("Kontonummer har fel format");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PayeeLifecareResult.create()).hasAllNullFieldsOrProperties();
	}
}
