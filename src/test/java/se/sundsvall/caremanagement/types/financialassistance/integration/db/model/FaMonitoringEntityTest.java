package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
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
import static java.time.Month.*;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FaMonitoringEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaMonitoringEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var startDate = LocalDate.of(2026, JULY, 1);
		final var endDate = LocalDate.of(2026, JULY, 31);
		final var entity = FaMonitoringEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withSource("LIFECARE")
			.withLifecareId("987654")
			.withTitle("Följ upp")
			.withDescription("Inväntar underlag")
			.withStartDate(startDate)
			.withEndDate(endDate)
			.withCreatedBy("joe01doe")
			.withCreated(created)
			.withUpdated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getSource()).isEqualTo("LIFECARE");
		assertThat(entity.getLifecareId()).isEqualTo("987654");
		assertThat(entity.getTitle()).isEqualTo("Följ upp");
		assertThat(entity.getStartDate()).isEqualTo(startDate);
		assertThat(entity.getEndDate()).isEqualTo(endDate);
		assertThat(entity.getCreatedBy()).isEqualTo("joe01doe");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaMonitoringEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FaMonitoringEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaMonitoringEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaMonitoringEntity.create();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
