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

class CreateFinancialAssistanceRequestTest {

	private static final FinancialAssistanceData DATA = FinancialAssistanceData.create().withApplicationType("NEW");

	@Test
	void testBean() {
		MatcherAssert.assertThat(CreateFinancialAssistanceRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Application for financial assistance")
			.withDescription("Renewal om rent")
			.withPriority("HIGH")
			.withReporterUserId("joe01doe")
			.withAssignedUserId("jane02doe")
			.withData(DATA);

		assertThat(request.getTitle()).isEqualTo("Application for financial assistance");
		assertThat(request.getDescription()).isEqualTo("Renewal om rent");
		assertThat(request.getPriority()).isEqualTo("HIGH");
		assertThat(request.getReporterUserId()).isEqualTo("joe01doe");
		assertThat(request.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(request.getData()).isEqualTo(DATA);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CreateFinancialAssistanceRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new CreateFinancialAssistanceRequest()).hasAllNullFieldsOrProperties();
	}

}
