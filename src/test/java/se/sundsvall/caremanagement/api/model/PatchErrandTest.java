package se.sundsvall.caremanagement.api.model;

import java.util.List;
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
		final var externalTags = List.of(ExternalTag.create());

		final var result = PatchErrand.create()
			.withTitle("t")
			.withCategory("c")
			.withType("ty")
			.withStatus("s")
			.withDescription("d")
			.withPriority("p")
			.withReporterUserId("r")
			.withAssignedUserId("a")
			.withContactReason("cr")
			.withContactReasonDescription("crd")
			.withExternalTags(externalTags);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getTitle()).isEqualTo("t");
		assertThat(result.getCategory()).isEqualTo("c");
		assertThat(result.getType()).isEqualTo("ty");
		assertThat(result.getStatus()).isEqualTo("s");
		assertThat(result.getDescription()).isEqualTo("d");
		assertThat(result.getPriority()).isEqualTo("p");
		assertThat(result.getReporterUserId()).isEqualTo("r");
		assertThat(result.getAssignedUserId()).isEqualTo("a");
		assertThat(result.getContactReason()).isEqualTo("cr");
		assertThat(result.getContactReasonDescription()).isEqualTo("crd");
		assertThat(result.getExternalTags()).isEqualTo(externalTags);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(PatchErrand.create()).hasAllNullFieldsOrProperties();
	}
}
