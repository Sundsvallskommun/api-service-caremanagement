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

class NormberakningRequestTest {

	@Test
	void testBean() {
		assertThat(NormberakningRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var request = NormberakningRequest.create()
			.withApplicant("198001012389")
			.withCoApplicant("198202022397")
			.withApplicationMonth("2026-06");

		assertThat(request.getApplicant()).isEqualTo("198001012389");
		assertThat(request.getCoApplicant()).isEqualTo("198202022397");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(NormberakningRequest.create()).hasAllNullFieldsOrProperties();
	}
}
