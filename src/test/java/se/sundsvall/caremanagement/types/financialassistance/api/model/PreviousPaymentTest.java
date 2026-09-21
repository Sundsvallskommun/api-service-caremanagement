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
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PreviousPaymentTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(10000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> List.of("item-" + new Random().nextInt()), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(PreviousPayment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = PreviousPayment.create()
			.withPayDate("2026-05-27")
			.withAmount(BigDecimal.valueOf(8500))
			.withConcernedMonth("2026-06")
			.withPaymentMethod("Bankkonto")
			.withName("Anna Andersson")
			.withClearing("1234")
			.withAccountNumber("5678901")
			.withMessage("Ekonomiskt bistånd");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPayDate()).isEqualTo("2026-05-27");
		assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(8500));
		assertThat(result.getConcernedMonth()).isEqualTo("2026-06");
		assertThat(result.getPaymentMethod()).isEqualTo("Bankkonto");
		assertThat(result.getName()).isEqualTo("Anna Andersson");
		assertThat(result.getClearing()).isEqualTo("1234");
		assertThat(result.getAccountNumber()).isEqualTo("5678901");
		assertThat(result.getMessage()).isEqualTo("Ekonomiskt bistånd");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PreviousPayment.create()).hasAllNullFieldsOrProperties();
	}
}
