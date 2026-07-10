package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
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

class FaIncomeTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaIncome.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var incomeType = "SALARY";
		final var amount = BigDecimal.valueOf(12345);
		final var incomeDate = LocalDate.of(2026, 5, 25);
		final var recipient = "APPLICANT";

		final var result = FaIncome.create()
			.withIncomeType(incomeType)
			.withAmount(amount)
			.withIncomeDate(incomeDate)
			.withRecipient(recipient);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getIncomeType()).isEqualTo(incomeType);
		assertThat(result.getAmount()).isEqualTo(amount);
		assertThat(result.getIncomeDate()).isEqualTo(incomeDate);
		assertThat(result.getRecipient()).isEqualTo(recipient);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaIncome.create()).hasAllNullFieldsOrProperties();
	}
}
