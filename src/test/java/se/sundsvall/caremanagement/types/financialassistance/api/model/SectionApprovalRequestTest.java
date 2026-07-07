package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class SectionApprovalRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(SectionApprovalRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var request = SectionApprovalRequest.create()
			.withApproved(true);

		assertThat(request.getApproved()).isTrue();
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		assertThat(SectionApprovalRequest.create()).hasAllNullFieldsOrProperties();
	}
}
