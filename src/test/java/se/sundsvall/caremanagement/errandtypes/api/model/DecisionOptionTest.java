package se.sundsvall.caremanagement.errandtypes.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DecisionOptionTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DecisionOption.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var option = DecisionOption.create()
			.withCode("BIFALL")
			.withDisplayName("Bifall")
			.withCarriesAmount(true);

		assertThat(option.getCode()).isEqualTo("BIFALL");
		assertThat(option.getDisplayName()).isEqualTo("Bifall");
		assertThat(option.isCarriesAmount()).isTrue();
		assertThat(option).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionOption.create()).hasAllNullFieldsOrPropertiesExcept("carriesAmount");
		assertThat(new DecisionOption()).hasAllNullFieldsOrPropertiesExcept("carriesAmount");
	}

}
