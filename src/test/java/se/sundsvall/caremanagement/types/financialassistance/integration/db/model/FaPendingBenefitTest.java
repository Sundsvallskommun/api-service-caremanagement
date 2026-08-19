package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaPendingBenefitTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaPendingBenefit.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var benefitName = "Bostadsbidrag";
		final var applicantName = "Anna Andersson";

		final var result = FaPendingBenefit.create()
			.withBenefitName(benefitName)
			.withApplicantName(applicantName);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getBenefitName()).isEqualTo(benefitName);
		assertThat(result.getApplicantName()).isEqualTo(applicantName);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaPendingBenefit.create()).hasAllNullFieldsOrProperties();
	}
}
