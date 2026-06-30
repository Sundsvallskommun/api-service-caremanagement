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

class FaSectionApprovalEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(FaSectionApprovalEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var when = OffsetDateTime.parse("2026-06-18T09:00:00Z");
		final var entity = FaSectionApprovalEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withSection("CALCULATION")
			.withApproved(true)
			.withApprovedBy("jane02doe")
			.withApprovedAt(when)
			.withCreated(when)
			.withUpdated(when);

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getSection()).isEqualTo("CALCULATION");
		org.assertj.core.api.Assertions.assertThat(entity.isApproved()).isTrue();
		org.assertj.core.api.Assertions.assertThat(entity.getApprovedBy()).isEqualTo("jane02doe");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(FaSectionApprovalEntity.create()).hasAllNullFieldsOrPropertiesExcept("approved");
		org.assertj.core.api.Assertions.assertThat(new FaSectionApprovalEntity()).hasAllNullFieldsOrPropertiesExcept("approved");
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaSectionApprovalEntity.create();
		entity.prePersist();
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isNotNull();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaSectionApprovalEntity.create();
		entity.preUpdate();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}
}
