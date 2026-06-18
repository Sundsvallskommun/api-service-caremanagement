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

class SectionApprovalRequestTest {

	@Test
	void testBean() {
		assertThat(SectionApprovalRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var request = SectionApprovalRequest.create()
			.withApproved(true)
			.withApprovedBy("jane02doe");

		org.assertj.core.api.Assertions.assertThat(request.getApproved()).isTrue();
		org.assertj.core.api.Assertions.assertThat(request.getApprovedBy()).isEqualTo("jane02doe");
		org.assertj.core.api.Assertions.assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(SectionApprovalRequest.create()).hasAllNullFieldsOrProperties();
	}
}
