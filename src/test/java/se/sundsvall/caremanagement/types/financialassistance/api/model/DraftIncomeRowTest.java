package se.sundsvall.caremanagement.types.financialassistance.api.model;

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
		final var typeId = 20;
		final var typeName = "Bostadsbidrag";
		final var applicantAmount = BigDecimal.valueOf(1850.0);
		final var applicantAmountDate = "2026-05-15T00:00:00Z";
		final var coApplicantAmount = BigDecimal.valueOf(0.0);
		final var coApplicantAmountDate = "2026-05-16T00:00:00Z";
		final var note = "SSBTEK: Bostadsbidrag";

		final var result = DraftIncomeRow.create()
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withApplicantAmount(applicantAmount)
			.withApplicantAmountDate(applicantAmountDate)
			.withCoApplicantAmount(coApplicantAmount)
			.withCoApplicantAmountDate(coApplicantAmountDate)
			.withNote(note);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getTypeId()).isEqualTo(typeId);
		assertThat(result.getTypeName()).isEqualTo(typeName);
		assertThat(result.getApplicantAmount()).isEqualTo(applicantAmount);
		assertThat(result.getApplicantAmountDate()).isEqualTo(applicantAmountDate);
		assertThat(result.getCoApplicantAmount()).isEqualTo(coApplicantAmount);
		assertThat(result.getCoApplicantAmountDate()).isEqualTo(coApplicantAmountDate);
		assertThat(result.getNote()).isEqualTo(note);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DraftIncomeRow.create()).hasAllNullFieldsOrProperties();
	}

}
