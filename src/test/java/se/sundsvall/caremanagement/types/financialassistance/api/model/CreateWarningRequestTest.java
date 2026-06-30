package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class CreateWarningRequestTest {

	@Test
	void testBean() {
		assertThat(CreateWarningRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = "UNHANDLED_INCOME";
		final var message = "Swish deposits: 2 400 kr";
		final var sourceKey = "Swish deposits";

		final var result = CreateWarningRequest.create()
			.withType(type)
			.withMessage(message)
			.withSourceKey(sourceKey);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getMessage()).isEqualTo(message);
		assertThat(result.getSourceKey()).isEqualTo(sourceKey);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CreateWarningRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new CreateWarningRequest()).hasAllNullFieldsOrProperties();
	}
}
