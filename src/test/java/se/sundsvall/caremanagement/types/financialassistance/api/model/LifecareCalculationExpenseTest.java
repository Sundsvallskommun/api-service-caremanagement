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

class LifecareCalculationExpenseTest {

	@Test
	void testBean() {
		assertThat(LifecareCalculationExpense.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var expense = LifecareCalculationExpense.create()
			.withType("Hyra")
			.withAppliedAmount(7500.0)
			.withApprovedAmount(7000.0);

		assertThat(expense.getType()).isEqualTo("Hyra");
		assertThat(expense.getAppliedAmount()).isEqualTo(7500.0);
		assertThat(expense.getApprovedAmount()).isEqualTo(7000.0);
	}

	@Test
	void testCreateReturnsBlankInstance() {
		assertThat(LifecareCalculationExpense.create()).hasAllNullFieldsOrProperties();
	}
}
