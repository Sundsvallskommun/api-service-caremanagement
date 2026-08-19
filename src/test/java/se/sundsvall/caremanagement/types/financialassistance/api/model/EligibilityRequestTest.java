package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class EligibilityRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(EligibilityRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var request = EligibilityRequest.create()
			.withApplicant("198001012389")
			.withCoApplicant("198202022397");

		assertThat(request.getApplicant()).isEqualTo("198001012389");
		assertThat(request.getCoApplicant()).isEqualTo("198202022397");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(EligibilityRequest.create()).hasAllNullFieldsOrProperties();
	}
}
