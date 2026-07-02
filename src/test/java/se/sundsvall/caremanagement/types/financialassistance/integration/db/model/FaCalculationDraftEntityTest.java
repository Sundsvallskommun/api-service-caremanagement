package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.Month.JUNE;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FaCalculationDraftEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaCalculationDraftEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var calculationFromDate = LocalDate.of(2026, JUNE, 1);
		final var calculationToDate = LocalDate.of(2026, JUNE, 30);
		final var calculationDate = LocalDate.of(2026, JUNE, 15);
		final var entity = FaCalculationDraftEntity.create()
			.withErrandId("errand")
			.withApplicationMonth("2026-06")
			.withNormId(7)
			.withNormType(List.of("NATIONAL_NORM"))
			.withCalculationFromDate(calculationFromDate)
			.withCalculationToDate(calculationToDate)
			.withCalculationDate(calculationDate)
			.withHasCustomHouseholdSize(true)
			.withHouseholdSize(3)
			.withCreated(created)
			.withUpdated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getNormId()).isEqualTo(7);
		assertThat(entity.getCalculationFromDate()).isEqualTo(calculationFromDate);
		assertThat(entity.getCalculationToDate()).isEqualTo(calculationToDate);
		assertThat(entity.getCalculationDate()).isEqualTo(calculationDate);
		assertThat(entity.getHasCustomHouseholdSize()).isTrue();
		assertThat(entity.getHouseholdSize()).isEqualTo(3);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaCalculationDraftEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FaCalculationDraftEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistAndPreUpdateSetTimestamps() {
		final var entity = FaCalculationDraftEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
