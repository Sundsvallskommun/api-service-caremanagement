package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NormberakningDraftTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> List.of(NormIncomeRow.create().withTypeName("type-" + new Random().nextInt())), List.class);
	}

	@Test
	void testBean() {
		assertThat(NormberakningDraft.class, allOf(
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
		final var normType = "normType";
		final var persons = List.of(NormPersonRow.create().withName("name"));
		final var incomes = List.of(NormIncomeRow.create().withTypeName("Bostadsbidrag"));
		final var expenses = List.of(NormExpenseRow.create().withCostType("rent"));
		final var incomeSum = BigDecimal.valueOf(1850.00);
		final var expenseSum = BigDecimal.valueOf(1000.00);
		final var created = now();
		final var updated = now();

		final var result = NormberakningDraft.create()
			.withErrandId(errandId)
			.withApplicationMonth(applicationMonth)
			.withNormId(normId)
			.withNormType(normType)
			.withPersons(persons)
			.withIncomes(incomes)
			.withExpenses(expenses)
			.withIncomeSum(incomeSum)
			.withExpenseSum(expenseSum)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getErrandId()).isEqualTo(errandId);
		assertThat(result.getApplicationMonth()).isEqualTo(applicationMonth);
		assertThat(result.getNormId()).isEqualTo(normId);
		assertThat(result.getNormType()).isEqualTo(normType);
		assertThat(result.getPersons()).isEqualTo(persons);
		assertThat(result.getIncomes()).isEqualTo(incomes);
		assertThat(result.getExpenses()).isEqualTo(expenses);
		assertThat(result.getIncomeSum()).isEqualTo(incomeSum);
		assertThat(result.getExpenseSum()).isEqualTo(expenseSum);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningDraft.create()).hasAllNullFieldsOrPropertiesExcept("persons", "incomes", "expenses");
		assertThat(new NormberakningDraft()).hasAllNullFieldsOrPropertiesExcept("persons", "incomes", "expenses");
	}
}
