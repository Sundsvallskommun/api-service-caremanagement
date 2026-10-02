package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareReminderOptionsTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareReminderOptions.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareReminderOptions.create()
			.withPriorities(List.of(LifecareReminderChoice.create()))
			.withStatuses(List.of(LifecareReminderChoice.create()))
			.withDefaultPriority(2)
			.withDefaultStatus(3);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPriorities()).isEqualTo(List.of(LifecareReminderChoice.create()));
		assertThat(result.getStatuses()).isEqualTo(List.of(LifecareReminderChoice.create()));
		assertThat(result.getDefaultPriority()).isEqualTo(2);
		assertThat(result.getDefaultStatus()).isEqualTo(3);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareReminderOptions.create()).hasAllNullFieldsOrProperties();
	}
}
