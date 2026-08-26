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
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaJobStimulusPeriodEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaJobStimulusPeriodEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-08-25T12:00:00Z");
		final var fromDate = LocalDate.of(2021, JANUARY, 1);
		final var toDate = LocalDate.of(2021, DECEMBER, 31);
		final var entity = FaJobStimulusPeriodEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withRole("APPLICANT")
			.withFromDate(fromDate)
			.withToDate(toDate)
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getRole()).isEqualTo("APPLICANT");
		assertThat(entity.getFromDate()).isEqualTo(fromDate);
		assertThat(entity.getToDate()).isEqualTo(toDate);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void prePersistStampsCreated() {
		final var entity = FaJobStimulusPeriodEntity.create();

		entity.prePersist();

		assertThat(entity.getCreated()).isNotNull();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaJobStimulusPeriodEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FaJobStimulusPeriodEntity()).hasAllNullFieldsOrProperties();
	}
}
