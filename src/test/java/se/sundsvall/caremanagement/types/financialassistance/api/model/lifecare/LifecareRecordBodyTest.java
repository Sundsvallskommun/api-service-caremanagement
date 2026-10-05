package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareRecordBodyTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareRecordBody.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareRecordBody.create()
			.withId("135")
			.withContent("<p>Hej</p>");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo("135");
		assertThat(result.getContent()).isEqualTo("<p>Hej</p>");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareRecordBody.create()).hasAllNullFieldsOrProperties();
	}
}
