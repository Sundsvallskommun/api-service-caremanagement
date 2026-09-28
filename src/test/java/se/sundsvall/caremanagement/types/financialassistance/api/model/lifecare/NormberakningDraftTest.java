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

class NormberakningDraftTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningDraft.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = buildBean();
		final var expected = buildBean();

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean).usingRecursiveComparison().isEqualTo(expected);
		assertThat(bean.getHasCustomHouseholdSize()).isTrue();
		assertThat(bean.getFinalized()).isTrue();
		assertThat(bean.getApplicantJobStimulus()).isTrue();
	}

	private static NormberakningDraft buildBean() {
		return NormberakningDraft.create()
			.withErrandId("value")
			.withApplicationMonth("value")
			.withNormId(1)
			.withNormType(List.of("value"))
			.withNormTypeDisplayNames(List.of("value"))
			.withCalculationFromDate("value")
			.withCalculationToDate("value")
			.withCalculationDate("value")
			.withHasCustomHouseholdSize(true)
			.withHouseholdSize(1)
			.withPersons(List.of(NormberakningPersonRow.create()))
			.withIncomes(List.of(NormberakningIncomeRow.create()))
			.withExpenses(List.of(NormberakningExpenseRow.create()))
			.withSpecialExpenses(List.of(NormberakningExpenseRow.create()))
			.withIncomeSum(BigDecimal.ONE)
			.withExpenseSum(BigDecimal.ONE)
			.withSpecialExpenseSum(BigDecimal.ONE)
			.withCreated("value")
			.withUpdated("value")
			.withSource("value")
			.withFinalized(true)
			.withAmountForHouseholdSize(BigDecimal.ONE)
			.withCommonHouseholdCost(BigDecimal.ONE)
			.withFamilyMembers(1)
			.withApplicantJobStimulus(true)
			.withNormRows(List.of(NormberakningNormRow.create()));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningDraft.create()).hasAllNullFieldsOrProperties();
	}
}
