package se.sundsvall.caremanagement.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ExternalTagTest {

	@Test
	void testBean() {
		assertThat(ExternalTag.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var key = "key";
		final var value = "value";

		final var result = ExternalTag.create().withKey(key).withValue(value);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ExternalTag.create()).hasAllNullFieldsOrProperties();
	}
}
