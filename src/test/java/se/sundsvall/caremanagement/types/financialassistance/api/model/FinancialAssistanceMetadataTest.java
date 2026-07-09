package se.sundsvall.caremanagement.types.financialassistance.api.model;

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

class FinancialAssistanceMetadataTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FinancialAssistanceMetadata.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var income = List.of(TypeOption.create().withCode("SALARY").withExternalDisplayName("Lön"));
		final var cost = List.of(TypeOption.create().withCode("RENT").withExternalDisplayName("Hyra (inte parkering/garage)"));

		final var metadata = FinancialAssistanceMetadata.create()
			.withIncomeTypes(income)
			.withCostTypes(cost);

		assertThat(metadata.getIncomeTypes()).isEqualTo(income);
		assertThat(metadata.getCostTypes()).isEqualTo(cost);
		assertThat(metadata).hasNoNullFieldsOrProperties();
	}

	@Test
	void testCreateReturnsEmptyInstance() {
		assertThat(FinancialAssistanceMetadata.create()).hasAllNullFieldsOrProperties();
	}
}
