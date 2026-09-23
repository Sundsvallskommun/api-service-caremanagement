package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FinalizePaymentTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FinalizePayment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var payee = Payee.create().withName("Anna Andersson").withPaymentMethod("BANKKONTO");
		final var payment = FinalizePayment.create()
			.withPaymentDate(LocalDate.of(2026, 6, 25))
			.withAmount(new BigDecimal("7900.00"))
			.withConcernedMonth("2026-06")
			.withPayee(payee)
			.withAccountingCode("5011")
			.withLocalPaymentNumber("4711")
			.withInvoiceNumber("2026-00417");

		assertThat(payment.getLocalPaymentNumber()).isEqualTo("4711");
		assertThat(payment.getInvoiceNumber()).isEqualTo("2026-00417");
		assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 25));
		assertThat(payment.getAmount()).isEqualByComparingTo("7900.00");
		assertThat(payment.getConcernedMonth()).isEqualTo("2026-06");
		assertThat(payment.getPayee()).isEqualTo(payee);
		assertThat(payment.getAccountingCode()).isEqualTo("5011");
		assertThat(payment).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FinalizePayment.create()).hasAllNullFieldsOrProperties();
	}
}
