package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class TypeOptionTest {

	@Test
	void testBean() {
		assertThat(TypeOption.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var option = TypeOption.create()
			.withCode("HOUSING_COST")
			.withDisplayName("Boendekostnad")
			.withCitizenReportable(true);

		org.assertj.core.api.Assertions.assertThat(option.getCode()).isEqualTo("HOUSING_COST");
		org.assertj.core.api.Assertions.assertThat(option.getDisplayName()).isEqualTo("Boendekostnad");
		org.assertj.core.api.Assertions.assertThat(option.isCitizenReportable()).isTrue();
		org.assertj.core.api.Assertions.assertThat(option).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(TypeOption.create()).hasAllNullFieldsOrPropertiesExcept("citizenReportable");
	}
}
