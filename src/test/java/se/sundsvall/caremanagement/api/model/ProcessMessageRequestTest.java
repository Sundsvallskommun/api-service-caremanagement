package se.sundsvall.caremanagement.api.model;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ProcessMessageRequestTest {

	@Test
	void testBean() {
		assertThat(ProcessMessageRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var messageName = "PaymentDecisionReceived";
		final var variables = Map.<String, Object>of("paymentDecision", "APPROVED");

		final var result = ProcessMessageRequest.create()
			.withMessageName(messageName)
			.withVariables(variables);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getMessageName()).isEqualTo(messageName);
		assertThat(result.getVariables()).isEqualTo(variables);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessMessageRequest.create()).hasAllNullFieldsOrProperties();
	}
}
