package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import java.math.BigDecimal;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaCostTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaCost.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("specification", "recipientOrPeriod"),
			hasValidBeanEqualsExcluding("specification", "recipientOrPeriod")));
	}

	@Test
	void testBuilderMethods() {
		final var costType = "RENT";
		final var appliedAmount = BigDecimal.valueOf(5400);
		final var otherSubType = "sub type";
		final var specification = "specification";
		final var recipientOrPeriod = "recipient or period";

		final var result = FaCost.create()
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
		assertThat(FaCost.create()).hasAllNullFieldsOrProperties();
	}
}
