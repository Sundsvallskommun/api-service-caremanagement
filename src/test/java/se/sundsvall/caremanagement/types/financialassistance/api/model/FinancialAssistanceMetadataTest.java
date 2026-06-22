package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FinancialAssistanceMetadataTest {

	@Test
	void testBean() {
		assertThat(FinancialAssistanceMetadata.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var income = List.of(TypeOption.create().withCode("SALARY_AFTER_TAX").withDisplayName("Lön efter skatt"));
		final var cost = List.of(TypeOption.create().withCode("HOUSING_COST").withDisplayName("Boendekostnad"));
		final var living = List.of(TypeOption.create().withCode("MEDICINE").withDisplayName("Medicin"));

		final var metadata = FinancialAssistanceMetadata.create()
			.withIncomeTypes(income)
			.withCostTypes(cost)
			.withLivingCostTypes(living);

		org.assertj.core.api.Assertions.assertThat(metadata.getIncomeTypes()).isEqualTo(income);
		org.assertj.core.api.Assertions.assertThat(metadata.getCostTypes()).isEqualTo(cost);
		org.assertj.core.api.Assertions.assertThat(metadata.getLivingCostTypes()).isEqualTo(living);
		org.assertj.core.api.Assertions.assertThat(metadata).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(FinancialAssistanceMetadata.create()).hasAllNullFieldsOrProperties();
	}
}
