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

class NormIncomeRowTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(NormIncomeRow.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var origin = "SYSTEM";
		final var position = 3;
		final var typeId = 20;
		final var typeName = "Bostadsbidrag";
		final var applicantProcessAmount = BigDecimal.valueOf(1850.00);
		final var applicantCaseworkerAmount = BigDecimal.valueOf(1900.00);
		final var applicantEffectiveAmount = BigDecimal.valueOf(1900.00);
		final var applicantAmountDate = now();
		final var coapplicantProcessAmount = BigDecimal.valueOf(950.00);
		final var coapplicantCaseworkerAmount = BigDecimal.valueOf(1000.00);
		final var coapplicantEffectiveAmount = BigDecimal.valueOf(1000.00);
		final var coapplicantAmountDate = now();
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var result = NormIncomeRow.create()
			.withId(id)
			.withOrigin(origin)
			.withPosition(position)
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withApplicantProcessAmount(applicantProcessAmount)
			.withApplicantCaseworkerAmount(applicantCaseworkerAmount)
			.withApplicantEffectiveAmount(applicantEffectiveAmount)
			.withApplicantAmountDate(applicantAmountDate)
			.withCoapplicantProcessAmount(coapplicantProcessAmount)
			.withCoapplicantCaseworkerAmount(coapplicantCaseworkerAmount)
			.withCoapplicantEffectiveAmount(coapplicantEffectiveAmount)
			.withCoapplicantAmountDate(coapplicantAmountDate)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getOrigin()).isEqualTo(origin);
		assertThat(result.getPosition()).isEqualTo(position);
		assertThat(result.getTypeId()).isEqualTo(typeId);
		assertThat(result.getTypeName()).isEqualTo(typeName);
		assertThat(result.getApplicantProcessAmount()).isEqualTo(applicantProcessAmount);
		assertThat(result.getApplicantCaseworkerAmount()).isEqualTo(applicantCaseworkerAmount);
		assertThat(result.getApplicantEffectiveAmount()).isEqualTo(applicantEffectiveAmount);
		assertThat(result.getApplicantAmountDate()).isEqualTo(applicantAmountDate);
		assertThat(result.getCoapplicantProcessAmount()).isEqualTo(coapplicantProcessAmount);
		assertThat(result.getCoapplicantCaseworkerAmount()).isEqualTo(coapplicantCaseworkerAmount);
		assertThat(result.getCoapplicantEffectiveAmount()).isEqualTo(coapplicantEffectiveAmount);
		assertThat(result.getCoapplicantAmountDate()).isEqualTo(coapplicantAmountDate);
		assertThat(result.isDeleted()).isEqualTo(deleted);
		assertThat(result.getNote()).isEqualTo(note);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormIncomeRow.create()).hasAllNullFieldsOrPropertiesExcept("deleted");
		assertThat(new NormIncomeRow()).hasAllNullFieldsOrPropertiesExcept("deleted");
	}
}
