package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
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
import static java.time.Month.AUGUST;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PaymentTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> BigDecimal.valueOf(new Random().nextInt(1_000_000)), BigDecimal.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Payment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-08-01T12:00:00Z");
		final var paymentDate = LocalDate.of(2026, AUGUST, 25);
		final var accountingDate = LocalDate.of(2026, AUGUST, 26);
		final var amount = new BigDecimal("4500.00");
		final var payment = Payment.create()
			.withId("id")
			.withSource("LIFECARE")
			.withLifecareId("987654")
			.withStatus("DRAFT")
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
			.withMessageLines(List.of("Ekonomiskt bistånd augusti 2026"))
			.withCreated(created)
			.withModified(created);

		assertThat(payment.getId()).isEqualTo("id");
		assertThat(payment.getSource()).isEqualTo("LIFECARE");
		assertThat(payment.getLifecareId()).isEqualTo("987654");
		assertThat(payment.getStatus()).isEqualTo("DRAFT");
		assertThat(payment.getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		assertThat(payment.getPaymentDate()).isEqualTo(paymentDate);
		assertThat(payment.getAmount()).isEqualTo(amount);
		assertThat(payment.getApplicationMonth()).isEqualTo("2026-08");
		assertThat(payment.getReportedOnStakeholderIds()).containsExactly("stakeholder-1");
		assertThat(payment.getAccountingDate()).isEqualTo(accountingDate);
		assertThat(payment.isExcludedFromPayment()).isTrue();
		assertThat(payment.getPayeeStakeholderId()).isEqualTo("stakeholder-1");
		assertThat(payment.getPaymentMethod()).isEqualTo("BANK_TRANSFER");
		assertThat(payment.getPayeeName()).isEqualTo("Anna Andersson");
		assertThat(payment.getPayeeAddress()).isEqualTo("Storgatan 1");
		assertThat(payment.getPayeeCareOf()).isEqualTo("c/o Bertil Bertilsson");
		assertThat(payment.getPayeeZipCode()).isEqualTo("85230");
		assertThat(payment.getPayeeCity()).isEqualTo("Sundsvall");
		assertThat(payment.getClearingNumber()).isEqualTo("8327-9");
		assertThat(payment.getAccountNumber()).isEqualTo("123 456 789-0");
		assertThat(payment.getLocalPaymentNumber()).isEqualTo("4711");
		assertThat(payment.getInvoiceNumber()).isEqualTo("2026-00417");
		assertThat(payment.isUsesOcr()).isTrue();
		assertThat(payment.getMessageLines()).containsExactly("Ekonomiskt bistånd augusti 2026");
		assertThat(payment.getCreated()).isEqualTo(created);
		assertThat(payment.getModified()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Payment.create()).hasAllNullFieldsOrPropertiesExcept("excludedFromPayment", "usesOcr");
	}
}
