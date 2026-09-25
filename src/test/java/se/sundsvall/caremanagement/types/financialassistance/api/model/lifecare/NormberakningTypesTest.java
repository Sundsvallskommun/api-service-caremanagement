package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class NormberakningTypesTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningTypes.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningTypes.create()
			.withNorms(List.of(NormberakningTypeOption.create()))
			.withIncomeTypes(List.of(NormberakningTypeOption.create()))
			.withCostTypes(List.of(NormberakningTypeOption.create()))
			.withLivingCostTypes(List.of(NormberakningTypeOption.create()));

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getNorms()).isEqualTo(List.of(NormberakningTypeOption.create()));
		assertThat(bean.getIncomeTypes()).isEqualTo(List.of(NormberakningTypeOption.create()));
		assertThat(bean.getCostTypes()).isEqualTo(List.of(NormberakningTypeOption.create()));
		assertThat(bean.getLivingCostTypes()).isEqualTo(List.of(NormberakningTypeOption.create()));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningTypes.create()).hasAllNullFieldsOrProperties();
	}
}
