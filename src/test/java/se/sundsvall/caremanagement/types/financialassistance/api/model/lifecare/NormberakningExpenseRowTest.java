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

class NormberakningExpenseRowTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningExpenseRow.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningExpenseRow.create()
			.withId("value")
			.withPosition(1)
			.withOrigin("value")
			.withBucket("value")
			.withCostType("value")
			.withCostTypeDisplayName("value")
			.withOtherSubType("value")
			.withSpecification("value")
			.withAppliedAmount(BigDecimal.ONE)
			.withProcessAmount(BigDecimal.ONE)
			.withCaseworkerAmount(BigDecimal.ONE)
			.withEffectiveAmount(BigDecimal.ONE)
			.withDeleted(true)
			.withNote("value");

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo("value");
		assertThat(bean.getPosition()).isEqualTo(1);
		assertThat(bean.getOrigin()).isEqualTo("value");
		assertThat(bean.getBucket()).isEqualTo("value");
		assertThat(bean.getCostType()).isEqualTo("value");
		assertThat(bean.getCostTypeDisplayName()).isEqualTo("value");
		assertThat(bean.getOtherSubType()).isEqualTo("value");
		assertThat(bean.getSpecification()).isEqualTo("value");
		assertThat(bean.getAppliedAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getProcessAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCaseworkerAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getEffectiveAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getDeleted()).isEqualTo(true);
		assertThat(bean.getNote()).isEqualTo("value");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningExpenseRow.create()).hasAllNullFieldsOrProperties();
	}
}
