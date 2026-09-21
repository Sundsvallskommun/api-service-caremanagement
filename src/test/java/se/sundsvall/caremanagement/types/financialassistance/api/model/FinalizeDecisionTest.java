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

class FinalizeDecisionTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FinalizeDecision.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var decision = FinalizeDecision.create()
			.withOutcome("BIFALL")
			.withReason("Inkomster enligt SSBTEK")
			.withPeriodFrom(LocalDate.of(2026, 6, 1))
			.withPeriodTo(LocalDate.of(2026, 6, 30))
			.withAmount(new BigDecimal("7900.00"))
			.withDecisionMessage("Du beviljas ekonomiskt bistånd");

		assertThat(decision.getOutcome()).isEqualTo("BIFALL");
		assertThat(decision.getReason()).isEqualTo("Inkomster enligt SSBTEK");
		assertThat(decision.getPeriodFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
		assertThat(decision.getPeriodTo()).isEqualTo(LocalDate.of(2026, 6, 30));
		assertThat(decision.getAmount()).isEqualByComparingTo("7900.00");
		assertThat(decision.getDecisionMessage()).isEqualTo("Du beviljas ekonomiskt bistånd");
		assertThat(decision).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FinalizeDecision.create()).hasAllNullFieldsOrProperties();
	}
}
