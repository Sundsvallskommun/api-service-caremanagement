package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
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

class FaMonitoringEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(FaMonitoringEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var startDate = LocalDate.of(2026, 7, 1);
		final var endDate = LocalDate.of(2026, 7, 31);
		final var entity = FaMonitoringEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withTitle("Följ upp")
			.withDescription("Inväntar underlag")
			.withStartDate(startDate)
			.withEndDate(endDate)
			.withCreatedBy("joe01doe")
			.withCreated(created)
			.withUpdated(created);

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getErrandId()).isEqualTo("errand");
		org.assertj.core.api.Assertions.assertThat(entity.getTitle()).isEqualTo("Följ upp");
		org.assertj.core.api.Assertions.assertThat(entity.getStartDate()).isEqualTo(startDate);
		org.assertj.core.api.Assertions.assertThat(entity.getEndDate()).isEqualTo(endDate);
		org.assertj.core.api.Assertions.assertThat(entity.getCreatedBy()).isEqualTo("joe01doe");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(FaMonitoringEntity.create()).hasAllNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(new FaMonitoringEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaMonitoringEntity.create();
		entity.prePersist();
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isNotNull();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaMonitoringEntity.create();
		entity.preUpdate();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}
}
