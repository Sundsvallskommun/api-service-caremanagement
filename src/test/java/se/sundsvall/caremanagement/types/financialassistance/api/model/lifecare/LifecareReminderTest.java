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

class LifecareReminderTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareReminder.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareReminder.create()
			.withId(40)
			.withDate("2026-09-30")
			.withStatus("Ej påbörjad")
			.withStatusCode(3)
			.withPriority("Normal")
			.withPriorityCode(2)
			.withType("Manuell bevakning insats")
			.withObjectType("IFO.Insats")
			.withText("Kontrollera hyran")
			.withCaseworker("Test Handläggare")
			.withCaseworkerId("TEST");

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(40);
		assertThat(result.getDate()).isEqualTo("2026-09-30");
		assertThat(result.getStatus()).isEqualTo("Ej påbörjad");
		assertThat(result.getStatusCode()).isEqualTo(3);
		assertThat(result.getPriority()).isEqualTo("Normal");
		assertThat(result.getPriorityCode()).isEqualTo(2);
		assertThat(result.getType()).isEqualTo("Manuell bevakning insats");
		assertThat(result.getObjectType()).isEqualTo("IFO.Insats");
		assertThat(result.getText()).isEqualTo("Kontrollera hyran");
		assertThat(result.getCaseworker()).isEqualTo("Test Handläggare");
		assertThat(result.getCaseworkerId()).isEqualTo("TEST");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareReminder.create()).hasAllNullFieldsOrProperties();
	}
}
