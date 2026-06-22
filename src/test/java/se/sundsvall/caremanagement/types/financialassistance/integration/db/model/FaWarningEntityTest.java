package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FaWarningEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(FaWarningEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
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

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getType()).isEqualTo("MISSING_SSBTEK");
		org.assertj.core.api.Assertions.assertThat(entity.getSourceKey()).isEqualTo("Dagersättning");
		org.assertj.core.api.Assertions.assertThat(entity.isAutoResolved()).isTrue();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(FaWarningEntity.create()).hasAllNullFieldsOrPropertiesExcept("autoResolved");
		org.assertj.core.api.Assertions.assertThat(new FaWarningEntity()).hasAllNullFieldsOrPropertiesExcept("autoResolved");
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaWarningEntity.create();
		entity.prePersist();
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isNotNull();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaWarningEntity.create();
		entity.preUpdate();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}
}
