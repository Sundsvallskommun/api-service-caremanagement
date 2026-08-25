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

class FormSnapshotOptionTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshotOption.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var option = FormSnapshotOption.create()
			.withCode("SINGLE")
			.withLabel("Ensamstående")
			.withSelected(true);

		assertThat(option.getCode()).isEqualTo("SINGLE");
		assertThat(option.getLabel()).isEqualTo("Ensamstående");
		assertThat(option.isSelected()).isTrue();
	}
}
