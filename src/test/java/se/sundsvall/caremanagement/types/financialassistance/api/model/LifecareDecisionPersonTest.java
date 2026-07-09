package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class LifecareDecisionPersonTest {

	@Test
	void testBean() {
		assertThat(LifecareDecisionPerson.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var person = LifecareDecisionPerson.create()
			.withPersonId("200001011234")
			.withName("Anna Andersson")
			.withCoApplicant(false);

		assertThat(person.getPersonId()).isEqualTo("200001011234");
		assertThat(person.getName()).isEqualTo("Anna Andersson");
		assertThat(person.getCoApplicant()).isFalse();
	}

	@Test
	void testCreateReturnsBlankInstance() {
		assertThat(LifecareDecisionPerson.create()).hasAllNullFieldsOrProperties();
	}
}
