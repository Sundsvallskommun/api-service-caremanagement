package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import static java.time.Month.AUGUST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PaymentRequestTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> BigDecimal.valueOf(new Random().nextInt(1_000_000)), BigDecimal.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(PaymentRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var paymentDate = LocalDate.of(2026, AUGUST, 25);
		final var accountingDate = LocalDate.of(2026, AUGUST, 26);
		final var amount = new BigDecimal("4500.00");
		final var request = PaymentRequest.create()
			.withSource("LIFECARE")
			.withLifecareId("987654")
			.withMoneyType("FORSORJNINGSSTOD")
			.withPaymentDate(paymentDate)
			.withAmount(amount)
			.withApplicationMonth("2026-08")
			.withReportedOnStakeholderIds(List.of("stakeholder-1"))
			.withAccountingDate(accountingDate)
			.withExcludedFromPayment(true)
			.withPayeeStakeholderId("stakeholder-1")
			.withPaymentMethod("BANK_TRANSFER")
			.withPayeeName("Anna Andersson")
			.withPayeeAddress("Storgatan 1")
			.withPayeeCareOf("c/o Bertil Bertilsson")
			.withPayeeZipCode("85230")
			.withPayeeCity("Sundsvall")
			.withClearingNumber("8327-9")
			.withAccountNumber("123 456 789-0")
			.withLocalPaymentNumber("4711")
			.withInvoiceNumber("2026-00417")
			.withUsesOcr(true)
			.withMessageLines(List.of("Ekonomiskt bistånd augusti 2026"));

		assertThat(request.getSource()).isEqualTo("LIFECARE");
		assertThat(request.getLifecareId()).isEqualTo("987654");
		assertThat(request.getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		assertThat(request.getPaymentDate()).isEqualTo(paymentDate);
		assertThat(request.getAmount()).isEqualTo(amount);
		assertThat(request.getApplicationMonth()).isEqualTo("2026-08");
		assertThat(request.getReportedOnStakeholderIds()).containsExactly("stakeholder-1");
		assertThat(request.getAccountingDate()).isEqualTo(accountingDate);
		assertThat(request.isExcludedFromPayment()).isTrue();
		assertThat(request.getPayeeStakeholderId()).isEqualTo("stakeholder-1");
		assertThat(request.getPaymentMethod()).isEqualTo("BANK_TRANSFER");
		assertThat(request.getPayeeName()).isEqualTo("Anna Andersson");
		assertThat(request.getPayeeAddress()).isEqualTo("Storgatan 1");
		assertThat(request.getPayeeCareOf()).isEqualTo("c/o Bertil Bertilsson");
		assertThat(request.getPayeeZipCode()).isEqualTo("85230");
		assertThat(request.getPayeeCity()).isEqualTo("Sundsvall");
		assertThat(request.getClearingNumber()).isEqualTo("8327-9");
		assertThat(request.getAccountNumber()).isEqualTo("123 456 789-0");
		assertThat(request.getLocalPaymentNumber()).isEqualTo("4711");
		assertThat(request.getInvoiceNumber()).isEqualTo("2026-00417");
		assertThat(request.isUsesOcr()).isTrue();
		assertThat(request.getMessageLines()).containsExactly("Ekonomiskt bistånd augusti 2026");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PaymentRequest.create()).hasAllNullFieldsOrPropertiesExcept("excludedFromPayment", "usesOcr");
	}
}
