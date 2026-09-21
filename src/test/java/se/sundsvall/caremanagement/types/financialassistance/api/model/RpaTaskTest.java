package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class RpaTaskTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(RpaTask.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var task = RpaTask.create()
			.withAction("WRITE_DECISION")
			.withReference("ns:errand-1:WRITE_DECISION")
			.withEnqueued(true);

		assertThat(task.getAction()).isEqualTo("WRITE_DECISION");
		assertThat(task.getReference()).isEqualTo("ns:errand-1:WRITE_DECISION");
		assertThat(task.getEnqueued()).isTrue();
		assertThat(task).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(RpaTask.create()).hasAllNullFieldsOrProperties();
	}
}
