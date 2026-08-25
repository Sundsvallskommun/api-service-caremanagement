package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaPlanningTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaPlanning.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("workDescription", "otherDescription"),
			hasValidBeanEqualsExcluding("workDescription", "otherDescription")));
	}

	@Test
	void testBuilderMethods() {
		final var person = "APPLICANT";
		final var planningType = "WORK";
		final var workExtent = "FULL_TIME";
		final var workDescription = "Job search";
		final var sickLeaveLevel = "HALF_TIME";
		final var sfiStudyPath = "B";
		final var sfiCourse = "C";
		final var otherDescription = "Other planning";

		final var result = FaPlanning.create()
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
		assertThat(FaPlanning.create()).hasAllNullFieldsOrProperties();
	}
}
