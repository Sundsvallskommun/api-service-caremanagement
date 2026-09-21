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

class PreviousDecisionTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(10000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> List.of("item-" + new Random().nextInt()), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(PreviousDecision.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = PreviousDecision.create()
			.withType("Bifall")
			.withReason("Försörjningsstöd")
			.withPeriodFrom("2026-05-01")
			.withPeriodTo("2026-05-31")
			.withAmount(BigDecimal.valueOf(8500))
			.withDate("2026-04-28");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getType()).isEqualTo("Bifall");
		assertThat(result.getReason()).isEqualTo("Försörjningsstöd");
		assertThat(result.getPeriodFrom()).isEqualTo("2026-05-01");
		assertThat(result.getPeriodTo()).isEqualTo("2026-05-31");
		assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(8500));
		assertThat(result.getDate()).isEqualTo("2026-04-28");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PreviousDecision.create()).hasAllNullFieldsOrProperties();
	}
}
