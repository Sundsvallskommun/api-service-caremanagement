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

class ParameterTest {

	@Test
	void testBean() {
		assertThat(Parameter.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var displayName = "displayName";
		final var parameterGroup = "group";
		final var key = "key";
		final var values = List.of("a", "b");

		final var result = Parameter.create()
			.withId(id)
			.withDisplayName(displayName)
			.withParameterGroup(parameterGroup)
			.withKey(key)
			.withValues(values);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getParameterGroup()).isEqualTo(parameterGroup);
		assertThat(result.getKey()).isEqualTo(key);
		assertThat(result.getValues()).isEqualTo(values);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Parameter.create()).hasAllNullFieldsOrProperties();
	}
}
