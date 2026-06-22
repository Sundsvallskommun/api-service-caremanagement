package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NormExpenseInputTest {

	@Test
	void testBean() {
		assertThat(NormExpenseInput.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var costType = "rent";
		final var bucket = "EXPENSE";
		final var otherSubType = "other";
		final var specification = "specification";
		final var appliedAmount = BigDecimal.valueOf(1200.00);
		final var caseworkerAmount = BigDecimal.valueOf(1100.00);
		final var note = "note";

		final var result = NormExpenseInput.create()
			.withCostType(costType)
			.withBucket(bucket)
			.withOtherSubType(otherSubType)
			.withSpecification(specification)
			.withAppliedAmount(appliedAmount)
			.withCaseworkerAmount(caseworkerAmount)
			.withNote(note);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getCostType()).isEqualTo(costType);
		assertThat(result.getBucket()).isEqualTo(bucket);
		assertThat(result.getOtherSubType()).isEqualTo(otherSubType);
		assertThat(result.getSpecification()).isEqualTo(specification);
		assertThat(result.getAppliedAmount()).isEqualTo(appliedAmount);
		assertThat(result.getCaseworkerAmount()).isEqualTo(caseworkerAmount);
		assertThat(result.getNote()).isEqualTo(note);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormExpenseInput.create()).hasAllNullFieldsOrProperties();
		assertThat(new NormExpenseInput()).hasAllNullFieldsOrProperties();
	}
}
