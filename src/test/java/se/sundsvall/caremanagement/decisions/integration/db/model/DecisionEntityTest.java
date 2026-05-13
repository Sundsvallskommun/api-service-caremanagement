package se.sundsvall.caremanagement.decisions.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
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
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testToString() {
		final var entity = DecisionEntity.create().withId("id").withErrandId("e1");
		org.assertj.core.api.Assertions.assertThat(entity.toString())
			.contains("DecisionEntity{").contains("id='id'").contains("errandId='e1'");
	}

	@Test
	void testBuilderMethods() {
		final var created = now();
		final var entity = DecisionEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withDecisionType("type")
			.withValue("value")
			.withDescription("desc")
			.withCreatedBy("user")
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getErrandId()).isEqualTo("errand");
		org.assertj.core.api.Assertions.assertThat(entity.getDecisionType()).isEqualTo("type");
		org.assertj.core.api.Assertions.assertThat(entity.getValue()).isEqualTo("value");
		org.assertj.core.api.Assertions.assertThat(entity.getDescription()).isEqualTo("desc");
		org.assertj.core.api.Assertions.assertThat(entity.getCreatedBy()).isEqualTo("user");
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(DecisionEntity.create()).hasAllNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(new DecisionEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsCreatedWhenMissing() {
		final var entity = new DecisionEntity();
		entity.prePersist();
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	void prePersistDoesNotOverwriteExistingCreated() {
		final var existing = now().minusDays(1);
		final var entity = DecisionEntity.create().withCreated(existing);
		entity.prePersist();
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isEqualTo(existing);
	}
}
