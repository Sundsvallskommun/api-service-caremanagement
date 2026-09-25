package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import java.math.BigDecimal;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class NormberakningPreviousPersonTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningPreviousPerson.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningPreviousPerson.create()
			.withName("value")
			.withAmount(BigDecimal.ONE)
			.withDeviationFromDate("value")
			.withDeviationToDate("value");

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getName()).isEqualTo("value");
		assertThat(bean.getAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getDeviationFromDate()).isEqualTo("value");
		assertThat(bean.getDeviationToDate()).isEqualTo("value");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningPreviousPerson.create()).hasAllNullFieldsOrProperties();
	}
}
