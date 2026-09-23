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

class PayeeTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(10000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> List.of("item-" + new Random().nextInt()), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Payee.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = Payee.create()
			.withId("a1b2c3d4-0000-0000-0000-000000000001")
			.withName("Anna Andersson")
			.withPaymentMethod("Bankkonto")
			.withClearing("1234")
			.withAccountNumber("5678901")
			.withLifecarePayeeId("1234567")
			.withAddress("Storgatan 1")
			.withCareOf("c/o Bertil Bertilsson")
			.withZipCode("85230")
			.withCity("Sundsvall");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getLifecarePayeeId()).isEqualTo("1234567");
		assertThat(result.getAddress()).isEqualTo("Storgatan 1");
		assertThat(result.getCareOf()).isEqualTo("c/o Bertil Bertilsson");
		assertThat(result.getZipCode()).isEqualTo("85230");
		assertThat(result.getCity()).isEqualTo("Sundsvall");
		assertThat(result.getId()).isEqualTo("a1b2c3d4-0000-0000-0000-000000000001");
		assertThat(result.getName()).isEqualTo("Anna Andersson");
		assertThat(result.getPaymentMethod()).isEqualTo("Bankkonto");
		assertThat(result.getClearing()).isEqualTo("1234");
		assertThat(result.getAccountNumber()).isEqualTo("5678901");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Payee.create()).hasAllNullFieldsOrProperties();
	}
}
