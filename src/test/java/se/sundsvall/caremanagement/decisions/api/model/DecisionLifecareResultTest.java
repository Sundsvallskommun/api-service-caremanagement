package se.sundsvall.caremanagement.decisions.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DecisionLifecareResultTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DecisionLifecareResult.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = DecisionLifecareResult.create()
			.withOutcome("WRITTEN")
			.withLifecareId("88123")
			.withDetail("Beslutet kunde inte registreras");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getOutcome()).isEqualTo("WRITTEN");
		assertThat(result.getLifecareId()).isEqualTo("88123");
		assertThat(result.getDetail()).isEqualTo("Beslutet kunde inte registreras");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DecisionLifecareResult.create()).hasAllNullFieldsOrProperties();
	}
}
