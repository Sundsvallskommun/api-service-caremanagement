package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FinalizeRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FinalizeRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var decision = FinalizeDecision.create().withOutcome("BIFALL").withAmount(new BigDecimal("7900"));
		final var communication = CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(false);
		final var payments = List.of(FinalizePayment.create().withConcernedMonth("2026-06"));

		final var request = FinalizeRequest.create()
			.withDecision(decision)
			.withCommunication(communication)
			.withPayments(payments)
			.withHouseholdSizeChanged(true);

		assertThat(request.getDecision()).isEqualTo(decision);
		assertThat(request.getCommunication()).isEqualTo(communication);
		assertThat(request.getPayments()).isEqualTo(payments);
		assertThat(request.getHouseholdSizeChanged()).isTrue();
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FinalizeRequest.create()).hasAllNullFieldsOrProperties();
	}
}
