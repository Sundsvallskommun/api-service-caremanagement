package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class NormIncomeInputTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormIncomeInput.class, allOf(
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
		final var applicantCaseworkerAmount = BigDecimal.valueOf(1900.00);
		final var applicantAmountDate = now();
		final var coapplicantCaseworkerAmount = BigDecimal.valueOf(1000.00);
		final var coapplicantAmountDate = now();
		final var note = "note";

		final var result = NormIncomeInput.create()
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withApplicantCaseworkerAmount(applicantCaseworkerAmount)
			.withApplicantAmountDate(applicantAmountDate)
			.withCoapplicantCaseworkerAmount(coapplicantCaseworkerAmount)
			.withCoapplicantAmountDate(coapplicantAmountDate)
			.withNote(note);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getTypeId()).isEqualTo(typeId);
		assertThat(result.getTypeName()).isEqualTo(typeName);
		assertThat(result.getApplicantCaseworkerAmount()).isEqualTo(applicantCaseworkerAmount);
		assertThat(result.getApplicantAmountDate()).isEqualTo(applicantAmountDate);
		assertThat(result.getCoapplicantCaseworkerAmount()).isEqualTo(coapplicantCaseworkerAmount);
		assertThat(result.getCoapplicantAmountDate()).isEqualTo(coapplicantAmountDate);
		assertThat(result.getNote()).isEqualTo(note);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormIncomeInput.create()).hasAllNullFieldsOrProperties();
		assertThat(new NormIncomeInput()).hasAllNullFieldsOrProperties();
	}
}
