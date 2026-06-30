package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class CalculationRequestTest {

	@Test
	void testBean() {
		assertThat(CalculationRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var request = CalculationRequest.create()
			.withApplicant("198001012389")
			.withCoApplicant("198202022397")
			.withApplicationMonth("2026-06")
			.withErrandId("cb20c51f-fcf3-42c0-b613-de563634a8ec")
			.withClassifiedIncomes("[{}]")
			.withUnhandledIncomes(List.of("u"))
			.withChangeWarnings(List.of("c"));

		assertThat(request.getApplicant()).isEqualTo("198001012389");
		assertThat(request.getCoApplicant()).isEqualTo("198202022397");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06");
		assertThat(request.getErrandId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");
		assertThat(request.getClassifiedIncomes()).isEqualTo("[{}]");
		assertThat(request.getUnhandledIncomes()).containsExactly("u");
		assertThat(request.getChangeWarnings()).containsExactly("c");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(CalculationRequest.create()).hasAllNullFieldsOrProperties();
	}
}
