package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareCalculationTest {

	private static final List<LifecareCalculationPerson> PERSONS = List.of(LifecareCalculationPerson.create().withPersonId("200001011234").withName("Anna Andersson"));
	private static final List<LifecareCalculationIncome> INCOMES = List.of(LifecareCalculationIncome.create().withType("Lön").withAmountApplicant(12000.0));
	private static final List<LifecareCalculationExpense> EXPENSES = List.of(LifecareCalculationExpense.create().withType("Hyra").withAppliedAmount(7500.0));
	private static final List<LifecareCalculationExpense> SPECIAL_EXPENSES = List.of(LifecareCalculationExpense.create().withType("Tandvård").withAppliedAmount(500.0));

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LifecareCalculationPerson.create().withPersonId("200001011234"), LifecareCalculationPerson.class);
		BeanMatchers.registerValueGenerator(() -> LifecareCalculationIncome.create().withType("Lön"), LifecareCalculationIncome.class);
		BeanMatchers.registerValueGenerator(() -> LifecareCalculationExpense.create().withType("Hyra"), LifecareCalculationExpense.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareCalculation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var calculation = LifecareCalculation.create()
			.withId(7001)
			.withNorm("Riksnorm 2026")
			.withFromDate("2026-06-01")
			.withToDate("2026-06-30")
			.withIncomeSum(12000.0)
			.withExpenseSum(9500.0)
			.withSpecialExpenseSum(500.0)
			.withNormSum(10500.0)
			.withCommonHouseholdCost(1200.0)
			.withFamilyCost(800.0)
			.withBalance(-2000.0)
			.withTotalSum(8500.0)
			.withIsFinal(true)
			.withPersons(PERSONS)
			.withIncomes(INCOMES)
			.withExpenses(EXPENSES)
			.withSpecialExpenses(SPECIAL_EXPENSES);

		assertThat(calculation.getId()).isEqualTo(7001);
		assertThat(calculation.getNorm()).isEqualTo("Riksnorm 2026");
		assertThat(calculation.getFromDate()).isEqualTo("2026-06-01");
		assertThat(calculation.getToDate()).isEqualTo("2026-06-30");
		assertThat(calculation.getIncomeSum()).isEqualTo(12000.0);
		assertThat(calculation.getExpenseSum()).isEqualTo(9500.0);
		assertThat(calculation.getSpecialExpenseSum()).isEqualTo(500.0);
		assertThat(calculation.getNormSum()).isEqualTo(10500.0);
		assertThat(calculation.getCommonHouseholdCost()).isEqualTo(1200.0);
		assertThat(calculation.getFamilyCost()).isEqualTo(800.0);
		assertThat(calculation.getBalance()).isEqualTo(-2000.0);
		assertThat(calculation.getTotalSum()).isEqualTo(8500.0);
		assertThat(calculation.getIsFinal()).isTrue();
		assertThat(calculation.getPersons()).isEqualTo(PERSONS);
		assertThat(calculation.getIncomes()).isEqualTo(INCOMES);
		assertThat(calculation.getExpenses()).isEqualTo(EXPENSES);
		assertThat(calculation.getSpecialExpenses()).isEqualTo(SPECIAL_EXPENSES);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareCalculation.create()).hasAllNullFieldsOrProperties();
	}
}
