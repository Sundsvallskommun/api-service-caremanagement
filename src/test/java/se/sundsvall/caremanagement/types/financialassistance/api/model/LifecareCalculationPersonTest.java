package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class LifecareCalculationPersonTest {

	@Test
	void testBean() {
		assertThat(LifecareCalculationPerson.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var person = LifecareCalculationPerson.create()
			.withPersonId("200001011234")
			.withName("Anna Andersson")
			.withAmount(4500.0)
			.withDeviationFromDate("2026-06-01")
			.withDeviationToDate("2026-06-30");

		assertThat(person.getPersonId()).isEqualTo("200001011234");
		assertThat(person.getName()).isEqualTo("Anna Andersson");
		assertThat(person.getAmount()).isEqualTo(4500.0);
		assertThat(person.getDeviationFromDate()).isEqualTo("2026-06-01");
		assertThat(person.getDeviationToDate()).isEqualTo("2026-06-30");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(LifecareCalculationPerson.create()).hasAllNullFieldsOrProperties();
	}
}
