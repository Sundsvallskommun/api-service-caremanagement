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

class LifecareReminderRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareReminderRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareReminderRequest.create()
			.withReminderDate("2026-09-30")
			.withText("Kontrollera hyran")
			.withPriority(2)
			.withStatus(3);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getReminderDate()).isEqualTo("2026-09-30");
		assertThat(result.getText()).isEqualTo("Kontrollera hyran");
		assertThat(result.getPriority()).isEqualTo(2);
		assertThat(result.getStatus()).isEqualTo(3);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareReminderRequest.create()).hasAllNullFieldsOrProperties();
	}
}
