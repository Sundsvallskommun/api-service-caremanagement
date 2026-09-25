package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class NormberakningPreviousCalculationTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningPreviousCalculation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningPreviousCalculation.create()
			.withId(1)
			.withNorm("value")
			.withFromDate("value")
			.withToDate("value")
			.withIncomeSum(BigDecimal.ONE)
			.withExpenseSum(BigDecimal.ONE)
			.withSpecialExpenseSum(BigDecimal.ONE)
			.withNormSum(BigDecimal.ONE)
			.withCommonHouseholdCost(BigDecimal.ONE)
			.withFamilyCost(BigDecimal.ONE)
			.withBalance(BigDecimal.ONE)
			.withTotalSum(BigDecimal.ONE)
			.withIsFinal(true)
			.withPersons(List.of(NormberakningPreviousPerson.create()))
			.withIncomes(List.of(NormberakningPreviousIncome.create()))
			.withExpenses(List.of(NormberakningPreviousExpense.create()))
			.withSpecialExpenses(List.of(NormberakningPreviousExpense.create()));

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(1);
		assertThat(bean.getNorm()).isEqualTo("value");
		assertThat(bean.getFromDate()).isEqualTo("value");
		assertThat(bean.getToDate()).isEqualTo("value");
		assertThat(bean.getIncomeSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getExpenseSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getSpecialExpenseSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getNormSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCommonHouseholdCost()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getFamilyCost()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getBalance()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getTotalSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getIsFinal()).isEqualTo(true);
		assertThat(bean.getPersons()).isEqualTo(List.of(NormberakningPreviousPerson.create()));
		assertThat(bean.getIncomes()).isEqualTo(List.of(NormberakningPreviousIncome.create()));
		assertThat(bean.getExpenses()).isEqualTo(List.of(NormberakningPreviousExpense.create()));
		assertThat(bean.getSpecialExpenses()).isEqualTo(List.of(NormberakningPreviousExpense.create()));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningPreviousCalculation.create()).hasAllNullFieldsOrProperties();
	}
}
