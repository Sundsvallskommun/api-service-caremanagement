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

class LifecareCalculationIncomeTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareCalculationIncome.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var income = LifecareCalculationIncome.create()
			.withType("Lön")
			.withAmountApplicant(12000.0)
			.withApplicantSearchDate("2026-05-15")
			.withAmountCoApplicant(0.0)
			.withCoApplicantSearchDate("2026-05-15");

		assertThat(income.getType()).isEqualTo("Lön");
		assertThat(income.getAmountApplicant()).isEqualTo(12000.0);
		assertThat(income.getApplicantSearchDate()).isEqualTo("2026-05-15");
		assertThat(income.getAmountCoApplicant()).isEqualTo(0.0);
		assertThat(income.getCoApplicantSearchDate()).isEqualTo("2026-05-15");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareCalculationIncome.create()).hasAllNullFieldsOrProperties();
	}
}
