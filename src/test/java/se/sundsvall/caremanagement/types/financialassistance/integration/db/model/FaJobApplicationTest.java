package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

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
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaJobApplicationTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaJobApplication.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var person = "APPLICANT";
		final var applicationDate = LocalDate.of(2026, MAY, 25);
		final var jobTitle = "Care assistant";
		final var employerAndPlace = "Sundsvall kommun";

		final var result = FaJobApplication.create()
			.withPerson(person)
			.withApplicationDate(applicationDate)
			.withJobTitle(jobTitle)
			.withEmployerAndPlace(employerAndPlace);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPerson()).isEqualTo(person);
		assertThat(result.getApplicationDate()).isEqualTo(applicationDate);
		assertThat(result.getJobTitle()).isEqualTo(jobTitle);
		assertThat(result.getEmployerAndPlace()).isEqualTo(employerAndPlace);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaJobApplication.create()).hasAllNullFieldsOrProperties();
	}
}
