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
		final var bean = NormberakningDraft.create()
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

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getErrandId()).isEqualTo("value");
		assertThat(bean.getApplicationMonth()).isEqualTo("value");
		assertThat(bean.getNormId()).isEqualTo(1);
		assertThat(bean.getNormType()).isEqualTo(List.of("value"));
		assertThat(bean.getNormTypeDisplayNames()).isEqualTo(List.of("value"));
		assertThat(bean.getCalculationFromDate()).isEqualTo("value");
		assertThat(bean.getCalculationToDate()).isEqualTo("value");
		assertThat(bean.getCalculationDate()).isEqualTo("value");
		assertThat(bean.getHasCustomHouseholdSize()).isEqualTo(true);
		assertThat(bean.getHouseholdSize()).isEqualTo(1);
		assertThat(bean.getPersons()).isEqualTo(List.of(NormberakningPersonRow.create()));
		assertThat(bean.getIncomes()).isEqualTo(List.of(NormberakningIncomeRow.create()));
		assertThat(bean.getExpenses()).isEqualTo(List.of(NormberakningExpenseRow.create()));
		assertThat(bean.getSpecialExpenses()).isEqualTo(List.of(NormberakningExpenseRow.create()));
		assertThat(bean.getIncomeSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getExpenseSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getSpecialExpenseSum()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCreated()).isEqualTo("value");
		assertThat(bean.getUpdated()).isEqualTo("value");
		assertThat(bean.getSource()).isEqualTo("value");
		assertThat(bean.getFinalized()).isEqualTo(true);
		assertThat(bean.getAmountForHouseholdSize()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCommonHouseholdCost()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getFamilyMembers()).isEqualTo(1);
		assertThat(bean.getApplicantJobStimulus()).isEqualTo(true);
		assertThat(bean.getNormRows()).isEqualTo(List.of(NormberakningNormRow.create()));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningDraft.create()).hasAllNullFieldsOrProperties();
	}
}
