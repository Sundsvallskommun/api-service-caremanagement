package se.sundsvall.caremanagement.formsnapshot.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FormSnapshotAnswerTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshotAnswer.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var answer = FormSnapshotAnswer.create()
			.withCode("SINGLE")
			.withValue("single")
			.withDisplay("Ensamstående");

		assertThat(answer.getCode()).isEqualTo("SINGLE");
		assertThat(answer.getValue()).isEqualTo("single");
		assertThat(answer.getDisplay()).isEqualTo("Ensamstående");
	}
}
