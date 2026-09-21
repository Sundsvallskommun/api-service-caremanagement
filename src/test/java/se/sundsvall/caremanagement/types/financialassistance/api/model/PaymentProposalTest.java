package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PaymentProposalTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(10000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> List.of("item-" + new Random().nextInt()), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(PaymentProposal.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = PaymentProposal.create()
			.withPayments(List.of(ProposedPayment.create().withConcernedMonth("2026-06")))
			.withPayeeOptions(List.of(Payee.create().withName("Anna")))
			.withPayeeSource("PREVIOUS_PAYMENT")
			.withPreviousPayment(PreviousPayment.create().withPayDate("2026-05-27"))
			.withExplanation("text")
			.withWarnings(List.of(Warning.create().withType("CO_APPLICANT_SPLIT_PAYMENT")));

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPayments()).isEqualTo(List.of(ProposedPayment.create().withConcernedMonth("2026-06")));
		assertThat(result.getPayeeOptions()).isEqualTo(List.of(Payee.create().withName("Anna")));
		assertThat(result.getPayeeSource()).isEqualTo("PREVIOUS_PAYMENT");
		assertThat(result.getPreviousPayment()).isEqualTo(PreviousPayment.create().withPayDate("2026-05-27"));
		assertThat(result.getExplanation()).isEqualTo("text");
		assertThat(result.getWarnings()).isEqualTo(List.of(Warning.create().withType("CO_APPLICANT_SPLIT_PAYMENT")));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PaymentProposal.create()).hasAllNullFieldsOrPropertiesExcept("payments", "payeeOptions", "warnings");
		assertThat(PaymentProposal.create().getPayments()).isEmpty();
		assertThat(PaymentProposal.create().getPayeeOptions()).isEmpty();
		assertThat(PaymentProposal.create().getWarnings()).isEmpty();
	}
}
