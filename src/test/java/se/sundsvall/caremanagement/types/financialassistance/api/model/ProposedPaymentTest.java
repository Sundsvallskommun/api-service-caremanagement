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

class ProposedPaymentTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(10000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> List.of("item-" + new Random().nextInt()), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(ProposedPayment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = ProposedPayment.create()
			.withPaymentDate(LocalDate.parse("2026-06-26"))
			.withAmount(BigDecimal.valueOf(4250))
			.withConcernedMonth("2026-06")
			.withPayee(Payee.create().withName("Anna Andersson"))
			.withAccountingCode("1234-5678");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPaymentDate()).isEqualTo(LocalDate.parse("2026-06-26"));
		assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(4250));
		assertThat(result.getConcernedMonth()).isEqualTo("2026-06");
		assertThat(result.getPayee()).isEqualTo(Payee.create().withName("Anna Andersson"));
		assertThat(result.getAccountingCode()).isEqualTo("1234-5678");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProposedPayment.create()).hasAllNullFieldsOrProperties();
	}
}
