package se.sundsvall.caremanagement.rpa.api.model;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class RpaTaskRequestTest {

	@Test
	void testBean() {
		assertThat(RpaTaskRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var action = "FETCH_SUPPLEMENTS";
		final var parameters = Map.of("k", "v");

		final var result = RpaTaskRequest.create()
			.withAction(action)
			.withParameters(parameters);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getAction()).isEqualTo(action);
		assertThat(result.getParameters()).isEqualTo(parameters);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(RpaTaskRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new RpaTaskRequest()).hasAllNullFieldsOrProperties();
	}
}
