package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.math.BigDecimal;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareCalculationExpenseTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareCalculationExpense.class, allOf(
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
			.withAppliedAmount(BigDecimal.valueOf(7500.0))
			.withApprovedAmount(BigDecimal.valueOf(7000.0));

		assertThat(expense.getType()).isEqualTo("Hyra");
		assertThat(expense.getAppliedAmount()).isEqualTo(BigDecimal.valueOf(7500.0));
		assertThat(expense.getApprovedAmount()).isEqualTo(BigDecimal.valueOf(7000.0));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareCalculationExpense.create()).hasAllNullFieldsOrProperties();
	}
}
