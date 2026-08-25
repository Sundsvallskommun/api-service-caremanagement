package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.Month.JUNE;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class CalculationDraftTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> List.of(NormIncomeRow.create().withTypeName("type-" + new Random().nextInt())), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(CalculationDraft.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var errandId = "errand";
		final var applicationMonth = "2026-06";
		final var normId = 1;
		final var normType = List.of("NATIONAL_NORM");
		final var calculationFromDate = LocalDate.of(2026, JUNE, 1);
		final var calculationToDate = LocalDate.of(2026, JUNE, 30);
		final var calculationDate = LocalDate.of(2026, JUNE, 15);
		final var hasCustomHouseholdSize = true;
		final var householdSize = 3;
		final var persons = List.of(NormPersonRow.create().withName("name"));
		final var incomes = List.of(NormIncomeRow.create().withTypeName("Bostadsbidrag"));
		final var expenses = List.of(NormExpenseRow.create().withCostType("rent"));
		final var specialExpenses = List.of(NormExpenseRow.create().withCostType("dental"));
		final var incomeSum = BigDecimal.valueOf(1850.00);
		final var expenseSum = BigDecimal.valueOf(1000.00);
		final var specialExpenseSum = BigDecimal.valueOf(500.00);
		final var created = now();
		final var updated = now();

		final var result = CalculationDraft.create()
			.withErrandId(errandId)
			.withApplicationMonth(applicationMonth)
			.withNormId(normId)
			.withNormType(normType)
			.withCalculationFromDate(calculationFromDate)
			.withCalculationToDate(calculationToDate)
			.withCalculationDate(calculationDate)
			.withHasCustomHouseholdSize(hasCustomHouseholdSize)
			.withHouseholdSize(householdSize)
			.withPersons(persons)
			.withIncomes(incomes)
			.withExpenses(expenses)
			.withSpecialExpenses(specialExpenses)
			.withIncomeSum(incomeSum)
			.withExpenseSum(expenseSum)
			.withSpecialExpenseSum(specialExpenseSum)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getErrandId()).isEqualTo(errandId);
		assertThat(result.getApplicationMonth()).isEqualTo(applicationMonth);
		assertThat(result.getNormId()).isEqualTo(normId);
		assertThat(result.getNormType()).isEqualTo(normType);
		assertThat(result.getCalculationFromDate()).isEqualTo(calculationFromDate);
		assertThat(result.getCalculationToDate()).isEqualTo(calculationToDate);
		assertThat(result.getCalculationDate()).isEqualTo(calculationDate);
		assertThat(result.getHasCustomHouseholdSize()).isEqualTo(hasCustomHouseholdSize);
		assertThat(result.getHouseholdSize()).isEqualTo(householdSize);
		assertThat(result.getPersons()).isEqualTo(persons);
		assertThat(result.getIncomes()).isEqualTo(incomes);
		assertThat(result.getExpenses()).isEqualTo(expenses);
		assertThat(result.getSpecialExpenses()).isEqualTo(specialExpenses);
		assertThat(result.getIncomeSum()).isEqualTo(incomeSum);
		assertThat(result.getExpenseSum()).isEqualTo(expenseSum);
		assertThat(result.getSpecialExpenseSum()).isEqualTo(specialExpenseSum);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CalculationDraft.create()).hasAllNullFieldsOrPropertiesExcept("persons", "incomes", "expenses", "specialExpenses");
		assertThat(new CalculationDraft()).hasAllNullFieldsOrPropertiesExcept("persons", "incomes", "expenses", "specialExpenses");
	}
}
