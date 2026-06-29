package se.sundsvall.caremanagement.core.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class PatchErrandTest {

	@Test
	void testBean() {
		assertThat(PatchErrand.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var patch = PatchErrand.create()
			.withTitle("title")
			.withStatus("OPEN")
			.withDescription("desc")
			.withPriority("HIGH")
			.withReporterUserId("reporter")
			.withAssignedUserId("assignee");

		assertThat(patch).hasNoNullFieldsOrProperties();
		assertThat(patch.getTitle()).isEqualTo("title");
		assertThat(patch.getStatus()).isEqualTo("OPEN");
		assertThat(patch.getDescription()).isEqualTo("desc");
		assertThat(patch.getPriority()).isEqualTo("HIGH");
		assertThat(patch.getReporterUserId()).isEqualTo("reporter");
		assertThat(patch.getAssignedUserId()).isEqualTo("assignee");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PatchErrand.create()).hasAllNullFieldsOrProperties();
	}
}
