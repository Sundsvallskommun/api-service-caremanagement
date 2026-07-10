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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PlanningTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Planning.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var person = "APPLICANT";
		final var planningType = "WORK";
		final var workExtent = "FULL";
		final var workDescription = "Permanent employment";
		final var sickLeaveLevel = "100";
		final var sfiStudyPath = "1";
		final var sfiCourse = "B";
		final var otherDescription = "Internship";

		final var result = Planning.create()
			.withPerson(person)
			.withPlanningType(planningType)
			.withWorkExtent(workExtent)
			.withWorkDescription(workDescription)
			.withSickLeaveLevel(sickLeaveLevel)
			.withSfiStudyPath(sfiStudyPath)
			.withSfiCourse(sfiCourse)
			.withOtherDescription(otherDescription);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPerson()).isEqualTo(person);
		assertThat(result.getPlanningType()).isEqualTo(planningType);
		assertThat(result.getWorkExtent()).isEqualTo(workExtent);
		assertThat(result.getWorkDescription()).isEqualTo(workDescription);
		assertThat(result.getSickLeaveLevel()).isEqualTo(sickLeaveLevel);
		assertThat(result.getSfiStudyPath()).isEqualTo(sfiStudyPath);
		assertThat(result.getSfiCourse()).isEqualTo(sfiCourse);
		assertThat(result.getOtherDescription()).isEqualTo(otherDescription);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Planning.create()).hasAllNullFieldsOrProperties();
	}
}
