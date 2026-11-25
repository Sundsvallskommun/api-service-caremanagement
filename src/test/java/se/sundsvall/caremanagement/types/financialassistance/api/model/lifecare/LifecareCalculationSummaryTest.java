package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

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

class LifecareCalculationSummaryTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareCalculationSummary.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = LifecareCalculationSummary.create()
			.withIncome(BigDecimal.ONE)
			.withJobStimulus(BigDecimal.ONE)
			.withJobStimulusDeduction(BigDecimal.ONE)
			.withNorm(BigDecimal.ONE)
			.withFamilyCost(BigDecimal.ONE)
			.withCommonHouseholdCost(BigDecimal.ONE)
			.withExpenses(BigDecimal.ONE)
			.withSum(BigDecimal.ONE)
			.withSpecialExpenses(BigDecimal.ONE)
			.withResult(BigDecimal.ONE);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getIncome()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getJobStimulus()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getJobStimulusDeduction()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getNorm()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getFamilyCost()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCommonHouseholdCost()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getExpenses()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getSpecialExpenses()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getResult()).isEqualTo(BigDecimal.ONE);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareCalculationSummary.create()).hasAllNullFieldsOrProperties();
	}
}
