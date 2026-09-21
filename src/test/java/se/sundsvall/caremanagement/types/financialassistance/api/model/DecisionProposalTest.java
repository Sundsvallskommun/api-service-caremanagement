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
import se.sundsvall.caremanagement.errandtypes.api.model.DecisionOption;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DecisionProposalTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(10000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> List.of("item-" + new Random().nextInt()), List.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(DecisionProposal.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = DecisionProposal.create()
			.withOutcome("BIFALL")
			.withOutcomeOptions(List.of(DecisionOption.create().withCode("BIFALL")))
			.withPeriodFrom(LocalDate.parse("2026-06-01"))
			.withPeriodTo(LocalDate.parse("2026-06-30"))
			.withConcernedMonth("2026-06")
			.withEstimatedAmount(BigDecimal.valueOf(4250))
			.withNormSum(BigDecimal.valueOf(6200))
			.withIncomeSum(BigDecimal.valueOf(3000))
			.withExpenseSum(BigDecimal.valueOf(800))
			.withSpecialExpenseSum(BigDecimal.valueOf(250))
			.withExplanation("text")
			.withReason("Försörjningsstöd")
			.withReasonOptions(List.of("Försörjningsstöd"))
			.withPhraseText("Bifall månad utan barn")
			.withPreviousDecision(PreviousDecision.create().withType("Bifall"))
			.withWarnings(List.of(Warning.create().withType("EXPENSE_PARTIALLY_REJECTED")));

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getOutcome()).isEqualTo("BIFALL");
		assertThat(result.getOutcomeOptions()).isEqualTo(List.of(DecisionOption.create().withCode("BIFALL")));
		assertThat(result.getPeriodFrom()).isEqualTo(LocalDate.parse("2026-06-01"));
		assertThat(result.getPeriodTo()).isEqualTo(LocalDate.parse("2026-06-30"));
		assertThat(result.getConcernedMonth()).isEqualTo("2026-06");
		assertThat(result.getEstimatedAmount()).isEqualTo(BigDecimal.valueOf(4250));
		assertThat(result.getNormSum()).isEqualTo(BigDecimal.valueOf(6200));
		assertThat(result.getIncomeSum()).isEqualTo(BigDecimal.valueOf(3000));
		assertThat(result.getExpenseSum()).isEqualTo(BigDecimal.valueOf(800));
		assertThat(result.getSpecialExpenseSum()).isEqualTo(BigDecimal.valueOf(250));
		assertThat(result.getExplanation()).isEqualTo("text");
		assertThat(result.getReason()).isEqualTo("Försörjningsstöd");
		assertThat(result.getReasonOptions()).isEqualTo(List.of("Försörjningsstöd"));
		assertThat(result.getPhraseText()).isEqualTo("Bifall månad utan barn");
		assertThat(result.getPreviousDecision()).isEqualTo(PreviousDecision.create().withType("Bifall"));
		assertThat(result.getWarnings()).isEqualTo(List.of(Warning.create().withType("EXPENSE_PARTIALLY_REJECTED")));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionProposal.create()).hasAllNullFieldsOrPropertiesExcept("outcomeOptions", "reasonOptions", "warnings");
		assertThat(DecisionProposal.create().getOutcomeOptions()).isEmpty();
		assertThat(DecisionProposal.create().getReasonOptions()).isEmpty();
		assertThat(DecisionProposal.create().getWarnings()).isEmpty();
	}
}
