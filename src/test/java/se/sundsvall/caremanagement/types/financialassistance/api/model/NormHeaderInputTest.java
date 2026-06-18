package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NormHeaderInputTest {

	@Test
	void testBean() {
		assertThat(NormHeaderInput.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var normId = 5;
		final var normType = "RIKSNORM";
		final var calculationFromDate = LocalDate.of(2026, 6, 1);
		final var calculationToDate = LocalDate.of(2026, 6, 30);
		final var calculationDate = LocalDate.of(2026, 6, 15);
		final var hasCustomHouseholdSize = true;
		final var householdSize = 1;

		final var result = NormHeaderInput.create()
			.withNormId(normId)
			.withNormType(normType)
			.withCalculationFromDate(calculationFromDate)
			.withCalculationToDate(calculationToDate)
			.withCalculationDate(calculationDate)
			.withHasCustomHouseholdSize(hasCustomHouseholdSize)
			.withHouseholdSize(householdSize);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getNormId()).isEqualTo(normId);
		assertThat(result.getNormType()).isEqualTo(normType);
		assertThat(result.getCalculationFromDate()).isEqualTo(calculationFromDate);
		assertThat(result.getCalculationToDate()).isEqualTo(calculationToDate);
		assertThat(result.getCalculationDate()).isEqualTo(calculationDate);
		assertThat(result.getHasCustomHouseholdSize()).isEqualTo(hasCustomHouseholdSize);
		assertThat(result.getHouseholdSize()).isEqualTo(householdSize);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormHeaderInput.create()).hasAllNullFieldsOrProperties();
		assertThat(new NormHeaderInput()).hasAllNullFieldsOrProperties();
	}
}
