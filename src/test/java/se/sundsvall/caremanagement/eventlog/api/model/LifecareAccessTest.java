package se.sundsvall.caremanagement.eventlog.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareAccessTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareAccess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var access = LifecareAccess.create()
			.withAction("CREATE")
			.withTarget("lifecare/journal-notes")
			.withDescription("Skrev en journalanteckning i Lifecare")
			.withLifecareId("4711");

		assertThat(access).hasNoNullFieldsOrProperties();
		assertThat(access.getAction()).isEqualTo("CREATE");
		assertThat(access.getTarget()).isEqualTo("lifecare/journal-notes");
		assertThat(access.getDescription()).isEqualTo("Skrev en journalanteckning i Lifecare");
		assertThat(access.getLifecareId()).isEqualTo("4711");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareAccess.create()).hasAllNullFieldsOrProperties();
	}
}
