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

class LifecareDocumentTypeTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareDocumentType.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareDocumentType.create()
			.withCode(1)
			.withName("EK Brev")
			.withCanChangeOccurenceDate(true)
			.withProtectedByDefault(true);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getCode()).isEqualTo(1);
		assertThat(result.getName()).isEqualTo("EK Brev");
		assertThat(result.getCanChangeOccurenceDate()).isEqualTo(true);
		assertThat(result.getProtectedByDefault()).isEqualTo(true);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareDocumentType.create()).hasAllNullFieldsOrProperties();
	}
}
