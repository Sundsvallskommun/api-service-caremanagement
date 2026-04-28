package se.sundsvall.caremanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class DecisionEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(DecisionEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create().withId("errand-id");
		final var decisionType = "PAYMENT";
		final var value = "APPROVED";
		final var description = "desc";
		final var createdBy = "jane01doe";
		final var created = now();

		final var entity = DecisionEntity.create()
			.withId(id)
			.withErrandEntity(errand)
			.withDecisionType(decisionType)
			.withValue(value)
			.withDescription(description)
			.withCreatedBy(createdBy)
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandEntity()).isEqualTo(errand);
		assertThat(entity.getDecisionType()).isEqualTo(decisionType);
		assertThat(entity.getValue()).isEqualTo(value);
		assertThat(entity.getDescription()).isEqualTo(description);
		assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void toStringHandlesNullErrand() {
		assertThat(DecisionEntity.create().toString()).contains("errandEntity=null");
	}

	@Test
	void prePersistSetsCreatedWhenNull() {
		final var entity = new DecisionEntity();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	void prePersistKeepsExistingCreated() {
		final var existing = OffsetDateTime.parse("2025-01-02T03:04:05+01:00");
		final var entity = DecisionEntity.create().withCreated(existing);
		entity.prePersist();
		assertThat(entity.getCreated()).isEqualTo(existing);
	}
}
