package se.sundsvall.caremanagement.decisions.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DecisionEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> BigDecimal.valueOf(new Random().nextInt(1_000_000)), BigDecimal.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(DecisionEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("description", "decisionMessage")));
	}

	@Test
	void testBuilderMethods() {
		final var created = FIXED_TIMESTAMP;
		final var amount = new BigDecimal("7900.00");
		final var decisionDate = LocalDate.parse("2026-06-18");
		final var periodFrom = LocalDate.parse("2026-06-01");
		final var periodTo = LocalDate.parse("2026-06-30");
		final var entity = DecisionEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withDecisionType("type")
			.withValue("value")
			.withDescription("desc")
			.withAmount(amount)
			.withDecisionMessage("message")
			.withDecisionDate(decisionDate)
			.withPeriodFrom(periodFrom)
			.withPeriodTo(periodTo)
			.withCreatedBy("user")
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getDecisionType()).isEqualTo("type");
		assertThat(entity.getValue()).isEqualTo("value");
		assertThat(entity.getDescription()).isEqualTo("desc");
		assertThat(entity.getAmount()).isEqualTo(amount);
		assertThat(entity.getDecisionMessage()).isEqualTo("message");
		assertThat(entity.getDecisionDate()).isEqualTo(decisionDate);
		assertThat(entity.getPeriodFrom()).isEqualTo(periodFrom);
		assertThat(entity.getPeriodTo()).isEqualTo(periodTo);
		assertThat(entity.getCreatedBy()).isEqualTo("user");
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsCreatedWhenMissing() {
		final var entity = new DecisionEntity();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	void prePersistDoesNotOverwriteExistingCreated() {
		final var existing = FIXED_TIMESTAMP.minusDays(1);
		final var entity = DecisionEntity.create().withCreated(existing);
		entity.prePersist();
		assertThat(entity.getCreated()).isEqualTo(existing);
	}
}
