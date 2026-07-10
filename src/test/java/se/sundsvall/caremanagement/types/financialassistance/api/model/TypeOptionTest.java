package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class TypeOptionTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(TypeOption.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var option = TypeOption.create()
			.withCode("RENT")
			.withExternalDisplayName("Hyra (inte parkering/garage)")
			.withInternalDisplayName("Boendekostnad")
			.withGroup("Boende")
			.withCitizenReportable(true);

		assertThat(option.getCode()).isEqualTo("RENT");
		assertThat(option.getExternalDisplayName()).isEqualTo("Hyra (inte parkering/garage)");
		assertThat(option.getInternalDisplayName()).isEqualTo("Boendekostnad");
		assertThat(option.getGroup()).isEqualTo("Boende");
		assertThat(option.isCitizenReportable()).isTrue();
		assertThat(option).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TypeOption.create()).hasAllNullFieldsOrPropertiesExcept("citizenReportable");
	}
}
