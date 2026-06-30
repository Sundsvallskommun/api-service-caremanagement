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
			.withCode("RENT")
			.withExternalDisplayName("Hyra (inte parkering/garage)")
			.withInternalDisplayName("Boendekostnad")
			.withGroup("Boende")
			.withCitizenReportable(true);

		org.assertj.core.api.Assertions.assertThat(option.getCode()).isEqualTo("RENT");
		org.assertj.core.api.Assertions.assertThat(option.getExternalDisplayName()).isEqualTo("Hyra (inte parkering/garage)");
		org.assertj.core.api.Assertions.assertThat(option.getInternalDisplayName()).isEqualTo("Boendekostnad");
		org.assertj.core.api.Assertions.assertThat(option.getGroup()).isEqualTo("Boende");
		org.assertj.core.api.Assertions.assertThat(option.isCitizenReportable()).isTrue();
		org.assertj.core.api.Assertions.assertThat(option).hasNoNullFieldsOrProperties();
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(TypeOption.create()).hasAllNullFieldsOrPropertiesExcept("citizenReportable");
	}
}
