package se.sundsvall.caremanagement.errandtypes.api.model;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FieldDescriptorTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FieldDescriptor.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var field = FieldDescriptor.create()
			.withName("maritalStatus")
			.withType("ENUM")
			.withRequired(true)
			.withOptions(List.of("SINGLE", "COHABITING"))
			.withItemsRef(null)
			.withAppliesTo(List.of("NEW", "RENEWAL"))
			.withCondition("periodChoice == OTHER_BENEFIT")
			.withDescription("Marital status of the applicant");

		assertThat(field.getName()).isEqualTo("maritalStatus");
		assertThat(field.getType()).isEqualTo("ENUM");
		assertThat(field.isRequired()).isTrue();
		assertThat(field.getOptions()).containsExactly("SINGLE", "COHABITING");
		assertThat(field.getItemsRef()).isNull();
		assertThat(field.getAppliesTo()).containsExactly("NEW", "RENEWAL");
		assertThat(field.getCondition()).isEqualTo("periodChoice == OTHER_BENEFIT");
		assertThat(field.getDescription()).isEqualTo("Marital status of the applicant");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FieldDescriptor.create()).hasAllNullFieldsOrPropertiesExcept("required");
		assertThat(new FieldDescriptor()).hasAllNullFieldsOrPropertiesExcept("required");
	}

}
