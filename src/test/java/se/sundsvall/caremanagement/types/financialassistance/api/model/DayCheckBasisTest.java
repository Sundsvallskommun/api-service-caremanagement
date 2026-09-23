package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.time.LocalDate;
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

class DayCheckBasisTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DayCheckBasis.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		// Arrange
		final var periods = List.of(new EconomicDecisionPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31)));
		final var consumedDays = 212;
		final var allDaysConsumed = false;

		// Act
		final var result = DayCheckBasis.create()
			.withEconomicDecisionPeriods(periods)
			.withConsumedDays(consumedDays)
			.withAllDaysConsumed(allDaysConsumed);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getEconomicDecisionPeriods()).isEqualTo(periods);
		assertThat(result.getConsumedDays()).isEqualTo(consumedDays);
		assertThat(result.getAllDaysConsumed()).isEqualTo(allDaysConsumed);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DayCheckBasis.create()).hasAllNullFieldsOrProperties();
	}
}
