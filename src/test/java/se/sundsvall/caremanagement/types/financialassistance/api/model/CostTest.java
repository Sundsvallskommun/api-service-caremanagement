package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.math.BigDecimal;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class CostTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(Cost.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var costType = "RENT";
		final var appliedAmount = BigDecimal.valueOf(5400);
		final var otherSubType = "OTHER";
		final var specification = "Dental care";
		final var recipientOrPeriod = "June 2026";

		final var result = Cost.create()
			.withCostType(costType)
			.withAppliedAmount(appliedAmount)
			.withOtherSubType(otherSubType)
			.withSpecification(specification)
			.withRecipientOrPeriod(recipientOrPeriod);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getCostType()).isEqualTo(costType);
		assertThat(result.getAppliedAmount()).isEqualTo(appliedAmount);
		assertThat(result.getOtherSubType()).isEqualTo(otherSubType);
		assertThat(result.getSpecification()).isEqualTo(specification);
		assertThat(result.getRecipientOrPeriod()).isEqualTo(recipientOrPeriod);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Cost.create()).hasAllNullFieldsOrProperties();
	}
}
