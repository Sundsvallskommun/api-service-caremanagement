package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
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

class FaSectionApprovalEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaSectionApprovalEntity.class, allOf(
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

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getSection()).isEqualTo("CALCULATION");
		assertThat(entity.isApproved()).isTrue();
		assertThat(entity.getApprovedBy()).isEqualTo("jane02doe");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaSectionApprovalEntity.create()).hasAllNullFieldsOrPropertiesExcept("approved");
		assertThat(new FaSectionApprovalEntity()).hasAllNullFieldsOrPropertiesExcept("approved");
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaSectionApprovalEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaSectionApprovalEntity.create();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
