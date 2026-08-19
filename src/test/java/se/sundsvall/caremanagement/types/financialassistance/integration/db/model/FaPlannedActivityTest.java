package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaPlannedActivityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaPlannedActivity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("activity"),
			hasValidBeanEqualsExcluding("activity")));
	}

	@Test
	void testBuilderMethods() {
		final var person = "APPLICANT";
		final var activity = "JOB_SEARCH";
		final var periodFrom = LocalDate.of(2026, MAY, 1);
		final var periodTo = LocalDate.of(2026, MAY, 31);

		final var result = FaPlannedActivity.create()
			.withPerson(person)
			.withActivity(activity)
			.withPeriodFrom(periodFrom)
			.withPeriodTo(periodTo);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPerson()).isEqualTo(person);
		assertThat(result.getActivity()).isEqualTo(activity);
		assertThat(result.getPeriodFrom()).isEqualTo(periodFrom);
		assertThat(result.getPeriodTo()).isEqualTo(periodTo);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaPlannedActivity.create()).hasAllNullFieldsOrProperties();
	}
}
