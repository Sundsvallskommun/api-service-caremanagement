package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PlannedActivityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(PlannedActivity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var person = "APPLICANT";
		final var activity = "Job coaching";
		final var periodFrom = LocalDate.of(2026, JUNE, 1);
		final var periodTo = LocalDate.of(2026, JUNE, 30);

		final var result = PlannedActivity.create()
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
		assertThat(PlannedActivity.create()).hasAllNullFieldsOrProperties();
	}
}
