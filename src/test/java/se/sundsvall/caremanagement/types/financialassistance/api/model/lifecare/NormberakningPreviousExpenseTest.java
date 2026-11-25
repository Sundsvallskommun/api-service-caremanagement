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

class NormberakningPreviousExpenseTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningPreviousExpense.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningPreviousExpense.create()
			.withType("value")
			.withAppliedAmount(BigDecimal.ONE)
			.withApprovedAmount(BigDecimal.ONE);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getType()).isEqualTo("value");
		assertThat(bean.getAppliedAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getApprovedAmount()).isEqualTo(BigDecimal.ONE);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningPreviousExpense.create()).hasAllNullFieldsOrProperties();
	}
}
