package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ActualisationRequestTest {

	@Test
	void testBean() {
		assertThat(ActualisationRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var request = ActualisationRequest.create()
			.withApplicant("198001012389")
			.withApplicationMonth("2026-06")
			.withErrandId("cb20c51f-fcf3-42c0-b613-de563634a8ec");

		assertThat(request.getApplicant()).isEqualTo("198001012389");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
		assertThat(request.getErrandId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");
	}

	@Test
	void testCreateReturnsBlankInstance() {
		assertThat(ActualisationRequest.create()).hasAllNullFieldsOrProperties();
	}
}
