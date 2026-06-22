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
		final var income = List.of(TypeOption.create().withCode("SALARY").withExternalDisplayName("Lön"));
		final var cost = List.of(TypeOption.create().withCode("RENT").withExternalDisplayName("Hyra (inte parkering/garage)"));

		final var metadata = FinancialAssistanceMetadata.create()
			.withIncomeTypes(income)
			.withCostTypes(cost);

		org.assertj.core.api.Assertions.assertThat(metadata.getIncomeTypes()).isEqualTo(income);
		org.assertj.core.api.Assertions.assertThat(metadata.getCostTypes()).isEqualTo(cost);
		org.assertj.core.api.Assertions.assertThat(metadata).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(FinancialAssistanceMetadata.create()).hasAllNullFieldsOrProperties();
	}
}
