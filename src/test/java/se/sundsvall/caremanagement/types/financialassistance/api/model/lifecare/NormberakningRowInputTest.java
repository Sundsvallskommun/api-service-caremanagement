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

class NormberakningRowInputTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningRowInput.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningRowInput.create()
			.withTypeId(1)
			.withTypeName("value")
			.withApplicantCaseworkerAmount(BigDecimal.ONE)
			.withApplicantAmountDate("value")
			.withCoapplicantCaseworkerAmount(BigDecimal.ONE)
			.withCoapplicantAmountDate("value")
			.withCostType("value")
			.withBucket("value")
			.withOtherSubType("value")
			.withSpecification("value")
			.withCaseworkerAmount(BigDecimal.ONE)
			.withAppliedAmount(BigDecimal.ONE)
			.withPartyId("value")
			.withRole("value")
			.withName("value")
			.withCaseworkerDays(1)
			.withIncluded(true)
			.withDeviationFromDate("value")
			.withDeviationToDate("value")
			.withNormInterval("value")
			.withNormRowId(1)
			.withNote("value");

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getTypeId()).isEqualTo(1);
		assertThat(bean.getTypeName()).isEqualTo("value");
		assertThat(bean.getApplicantCaseworkerAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getApplicantAmountDate()).isEqualTo("value");
		assertThat(bean.getCoapplicantCaseworkerAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getCoapplicantAmountDate()).isEqualTo("value");
		assertThat(bean.getCostType()).isEqualTo("value");
		assertThat(bean.getBucket()).isEqualTo("value");
		assertThat(bean.getOtherSubType()).isEqualTo("value");
		assertThat(bean.getSpecification()).isEqualTo("value");
		assertThat(bean.getCaseworkerAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getAppliedAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getPartyId()).isEqualTo("value");
		assertThat(bean.getRole()).isEqualTo("value");
		assertThat(bean.getName()).isEqualTo("value");
		assertThat(bean.getCaseworkerDays()).isEqualTo(1);
		assertThat(bean.getIncluded()).isEqualTo(true);
		assertThat(bean.getDeviationFromDate()).isEqualTo("value");
		assertThat(bean.getDeviationToDate()).isEqualTo("value");
		assertThat(bean.getNormInterval()).isEqualTo("value");
		assertThat(bean.getNormRowId()).isEqualTo(1);
		assertThat(bean.getNote()).isEqualTo("value");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningRowInput.create()).hasAllNullFieldsOrProperties();
	}
}
