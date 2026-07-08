package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FaWarningEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaWarningEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("message"),
			hasValidBeanEqualsExcluding("message")));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var entity = FaWarningEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withType("MISSING_SSBTEK")
			.withSourceKey("Dagersättning")
			.withMessage("Still missing in SSBTEK: Dagersättning")
			.withStatus("OPEN")
			.withAutoResolved(true)
			.withCreated(created)
			.withUpdated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getType()).isEqualTo("MISSING_SSBTEK");
		assertThat(entity.getSourceKey()).isEqualTo("Dagersättning");
		assertThat(entity.isAutoResolved()).isTrue();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaWarningEntity.create()).hasAllNullFieldsOrPropertiesExcept("autoResolved");
		assertThat(new FaWarningEntity()).hasAllNullFieldsOrPropertiesExcept("autoResolved");
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaWarningEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaWarningEntity.create();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
