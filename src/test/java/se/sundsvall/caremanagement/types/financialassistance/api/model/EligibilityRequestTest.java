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

class EligibilityRequestTest {

	@Test
	void testBean() {
		assertThat(EligibilityRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var request = EligibilityRequest.create()
			.withApplicant("198001012389")
			.withCoApplicant("198202022397")
			.withWithinDays(30);

		assertThat(request.getApplicant()).isEqualTo("198001012389");
		assertThat(request.getCoApplicant()).isEqualTo("198202022397");
		assertThat(request.getWithinDays()).isEqualTo(30);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(EligibilityRequest.create()).hasAllNullFieldsOrProperties();
	}
}
