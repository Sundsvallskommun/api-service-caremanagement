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

class NormberakningIncomeRowTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningIncomeRow.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningIncomeRow.create()
			.withId("value")
			.withPosition(1)
			.withOrigin("value")
			.withTypeId(1)
			.withTypeName("value")
			.withApplicantProcessAmount(BigDecimal.ONE)
			.withApplicantCaseworkerAmount(BigDecimal.ONE)
			.withApplicantEffectiveAmount(BigDecimal.ONE)
			.withApplicantAmountDate("value")
			.withApplicantJobStimulus(true)
			.withApplicantCountedAmount(BigDecimal.ONE)
			.withCoapplicantProcessAmount(BigDecimal.ONE)
			.withCoapplicantCaseworkerAmount(BigDecimal.ONE)
			.withCoapplicantEffectiveAmount(BigDecimal.ONE)
			.withCoapplicantAmountDate("value")
			.withDeleted(true)
			.withNote("value");

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo("value");
		assertThat(bean.getPosition()).isEqualTo(1);
		assertThat(bean.getOrigin()).isEqualTo("value");
		assertThat(bean.getTypeId()).isEqualTo(1);
		assertThat(bean.getTypeName()).isEqualTo("value");
		assertThat(bean.getApplicantProcessAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getApplicantCaseworkerAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getApplicantEffectiveAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getApplicantAmountDate()).isEqualTo("value");
		assertThat(bean.getApplicantJobStimulus()).isEqualTo(true);
		assertThat(bean.getApplicantCountedAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCoapplicantProcessAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCoapplicantCaseworkerAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCoapplicantEffectiveAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCoapplicantAmountDate()).isEqualTo("value");
		assertThat(bean.getDeleted()).isEqualTo(true);
		assertThat(bean.getNote()).isEqualTo("value");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningIncomeRow.create()).hasAllNullFieldsOrProperties();
	}
}
