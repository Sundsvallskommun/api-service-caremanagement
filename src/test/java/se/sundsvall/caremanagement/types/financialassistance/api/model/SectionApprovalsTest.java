package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class SectionApprovalsTest {

	@Test
	void testBean() {
		assertThat(SectionApprovals.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var calculation = SectionApproval.create().withSection("CALCULATION").withApproved(true);
		final var payment = SectionApproval.create().withSection("PAYMENT").withApproved(false);
		final var decision = SectionApproval.create().withSection("DECISION").withApproved(false);

		final var approvals = SectionApprovals.create()
			.withCalculation(calculation)
			.withPayment(payment)
			.withDecision(decision);

		org.assertj.core.api.Assertions.assertThat(approvals.getCalculation()).isEqualTo(calculation);
		org.assertj.core.api.Assertions.assertThat(approvals.getPayment()).isEqualTo(payment);
		org.assertj.core.api.Assertions.assertThat(approvals.getDecision()).isEqualTo(decision);
		org.assertj.core.api.Assertions.assertThat(approvals).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(SectionApprovals.create()).hasAllNullFieldsOrProperties();
	}
}
