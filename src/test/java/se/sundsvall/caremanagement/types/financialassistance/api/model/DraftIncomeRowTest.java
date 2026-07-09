package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DraftIncomeRowTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DraftIncomeRow.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var row = DraftIncomeRow.create()
			.withTypeId(20)
			.withTypeName("Bostadsbidrag")
			.withApplicantAmount(1850.0)
			.withApplicantAmountDate("2026-05-15T00:00:00Z")
			.withCoApplicantAmount(0.0)
			.withCoApplicantAmountDate(null)
			.withNote("SSBTEK: Bostadsbidrag");

		assertThat(row.getTypeId()).isEqualTo(20);
		assertThat(row.getTypeName()).isEqualTo("Bostadsbidrag");
		assertThat(row.getApplicantAmount()).isEqualTo(1850.0);
		assertThat(row.getNote()).isEqualTo("SSBTEK: Bostadsbidrag");
	}
}
