package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
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

class PayeeOptionTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(PayeeOption.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-09-21T12:00:00Z");

		final var result = PayeeOption.create()
			.withId("id")
			.withName("Sundsvalls Hyresbostäder AB")
			.withPaymentMethod("Bankgiro via Plusgiro")
			.withClearing("6000")
			.withAccountNumber("5555-6666")
			.withSource("MANUAL")
			.withLifecareStatus("PENDING")
			.withLifecarePayeeId("44213")
			.withLifecareDetail("Kontonummer har fel format")
			.withLastPaidOn("2026-08-27")
			.withCreated(created);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo("id");
		assertThat(result.getName()).isEqualTo("Sundsvalls Hyresbostäder AB");
		assertThat(result.getPaymentMethod()).isEqualTo("Bankgiro via Plusgiro");
		assertThat(result.getClearing()).isEqualTo("6000");
		assertThat(result.getAccountNumber()).isEqualTo("5555-6666");
		assertThat(result.getSource()).isEqualTo("MANUAL");
		assertThat(result.getLifecareStatus()).isEqualTo("PENDING");
		assertThat(result.getLifecarePayeeId()).isEqualTo("44213");
		assertThat(result.getLifecareDetail()).isEqualTo("Kontonummer har fel format");
		assertThat(result.getLastPaidOn()).isEqualTo("2026-08-27");
		assertThat(result.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PayeeOption.create()).hasAllNullFieldsOrProperties();
	}
}
