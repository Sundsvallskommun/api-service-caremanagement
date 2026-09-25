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

class NormberakningPersonRowTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormberakningPersonRow.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var bean = NormberakningPersonRow.create()
			.withId("value")
			.withPosition(1)
			.withOrigin("value")
			.withPartyId("value")
			.withPersonalNumber("value")
			.withRole("value")
			.withRoleDisplayName("value")
			.withName("value")
			.withProcessDays(1)
			.withCaseworkerDays(1)
			.withEffectiveDays(1)
			.withIncluded(true)
			.withDeviationFromDate("value")
			.withDeviationToDate("value")
			.withNormInterval("value")
			.withNormRowId(1)
			.withAmount(BigDecimal.ONE)
			.withDeleted(true)
			.withNote("value");

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo("value");
		assertThat(bean.getPosition()).isEqualTo(1);
		assertThat(bean.getOrigin()).isEqualTo("value");
		assertThat(bean.getPartyId()).isEqualTo("value");
		assertThat(bean.getPersonalNumber()).isEqualTo("value");
		assertThat(bean.getRole()).isEqualTo("value");
		assertThat(bean.getRoleDisplayName()).isEqualTo("value");
		assertThat(bean.getName()).isEqualTo("value");
		assertThat(bean.getProcessDays()).isEqualTo(1);
		assertThat(bean.getCaseworkerDays()).isEqualTo(1);
		assertThat(bean.getEffectiveDays()).isEqualTo(1);
		assertThat(bean.getIncluded()).isEqualTo(true);
		assertThat(bean.getDeviationFromDate()).isEqualTo("value");
		assertThat(bean.getDeviationToDate()).isEqualTo("value");
		assertThat(bean.getNormInterval()).isEqualTo("value");
		assertThat(bean.getNormRowId()).isEqualTo(1);
		assertThat(bean.getAmount()).isEqualTo(BigDecimal.ONE);
		assertThat(bean.getDeleted()).isEqualTo(true);
		assertThat(bean.getNote()).isEqualTo("value");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormberakningPersonRow.create()).hasAllNullFieldsOrProperties();
	}
}
