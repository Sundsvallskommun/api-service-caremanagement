package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

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
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.Month.AUGUST;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaPaymentEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> BigDecimal.valueOf(new Random().nextInt(1_000_000)), BigDecimal.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaPaymentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("reportedOnStakeholderIds", "messageLines"),
			hasValidBeanEqualsExcluding("reportedOnStakeholderIds", "messageLines"),
			hasValidBeanToStringExcluding("reportedOnStakeholderIds", "messageLines")));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-08-01T12:00:00Z");
		final var paymentDate = LocalDate.of(2026, AUGUST, 25);
		final var accountingDate = LocalDate.of(2026, AUGUST, 26);
		final var amount = new BigDecimal("4500.00");
		final var entity = FaPaymentEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withSource("LIFECARE")
			.withLifecareId("987654")
			.withStatus("DRAFT")
			.withMoneyType("FORSORJNINGSSTOD")
			.withPaymentDate(paymentDate)
			.withAmount(amount)
			.withApplicationMonth("2026-08")
			.withAccountingCode("4631-1234")
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

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getSource()).isEqualTo("LIFECARE");
		assertThat(entity.getLifecareId()).isEqualTo("987654");
		assertThat(entity.getStatus()).isEqualTo("DRAFT");
		assertThat(entity.getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		assertThat(entity.getPaymentDate()).isEqualTo(paymentDate);
		assertThat(entity.getAmount()).isEqualTo(amount);
		assertThat(entity.getApplicationMonth()).isEqualTo("2026-08");
		assertThat(entity.getReportedOnStakeholderIds()).containsExactly("stakeholder-1");
		assertThat(entity.getAccountingDate()).isEqualTo(accountingDate);
		assertThat(entity.isExcludedFromPayment()).isTrue();
		assertThat(entity.getPayeeStakeholderId()).isEqualTo("stakeholder-1");
		assertThat(entity.getPaymentMethod()).isEqualTo("BANK_TRANSFER");
		assertThat(entity.getPayeeName()).isEqualTo("Anna Andersson");
		assertThat(entity.getPayeeAddress()).isEqualTo("Storgatan 1");
		assertThat(entity.getPayeeCareOf()).isEqualTo("c/o Bertil Bertilsson");
		assertThat(entity.getPayeeZipCode()).isEqualTo("85230");
		assertThat(entity.getPayeeCity()).isEqualTo("Sundsvall");
		assertThat(entity.getClearingNumber()).isEqualTo("8327-9");
		assertThat(entity.getAccountNumber()).isEqualTo("123 456 789-0");
		assertThat(entity.getLocalPaymentNumber()).isEqualTo("4711");
		assertThat(entity.getInvoiceNumber()).isEqualTo("2026-00417");
		assertThat(entity.isUsesOcr()).isTrue();
		assertThat(entity.getMessageLines()).containsExactly("Ekonomiskt bistånd augusti 2026");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaPaymentEntity.create()).hasAllNullFieldsOrPropertiesExcept("excludedFromPayment", "usesOcr");
		assertThat(new FaPaymentEntity()).hasAllNullFieldsOrPropertiesExcept("excludedFromPayment", "usesOcr");
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaPaymentEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getModified()).isNotNull();
	}

	@Test
	void preUpdateSetsModified() {
		final var entity = FaPaymentEntity.create();
		entity.preUpdate();
		assertThat(entity.getModified()).isNotNull();
	}
}
