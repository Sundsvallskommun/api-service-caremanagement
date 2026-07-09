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
	void testSettersWork() {
		final var option = DecisionOption.create();
		option.setCode("AVSLAG");
		option.setDisplayName("Avslag");
		option.setCarriesAmount(false);

		assertThat(option.getCode()).isEqualTo("AVSLAG");
		assertThat(option.getDisplayName()).isEqualTo("Avslag");
		assertThat(option.isCarriesAmount()).isFalse();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionOption.create()).hasAllNullFieldsOrPropertiesExcept("carriesAmount");
		assertThat(new DecisionOption()).hasAllNullFieldsOrPropertiesExcept("carriesAmount");
	}

	@Test
	void testEqualsHashCodeAndToString() {
		final var a = DecisionOption.create().withCode("BIFALL").withDisplayName("Bifall").withCarriesAmount(true);
		final var b = DecisionOption.create().withCode("BIFALL").withDisplayName("Bifall").withCarriesAmount(true);
		final var c = DecisionOption.create().withCode("AVSLAG").withDisplayName("Avslag").withCarriesAmount(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string")
			.isEqualTo(a)
			.hasToString(b.toString());
		assertThat(a.toString()).contains("BIFALL", "Bifall");
	}
}
