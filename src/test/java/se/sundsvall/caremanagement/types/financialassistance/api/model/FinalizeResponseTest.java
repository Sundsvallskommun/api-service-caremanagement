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

class FinalizeResponseTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FinalizeResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var communication = CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(false);

		final var response = FinalizeResponse.create()
			.withDecisionId("decision-1")
			.withProcessMessageCorrelated(true)
			.withCommunication(communication);

		assertThat(response.getDecisionId()).isEqualTo("decision-1");
		assertThat(response.getProcessMessageCorrelated()).isTrue();
		assertThat(response.getCommunication()).isEqualTo(communication);
		assertThat(response).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FinalizeResponse.create()).hasAllNullFieldsOrProperties();
	}
}
