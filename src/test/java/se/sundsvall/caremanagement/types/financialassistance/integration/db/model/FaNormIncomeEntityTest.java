package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FaNormIncomeEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(FaNormIncomeEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var errandId = "errand";
		final var origin = "SYSTEM";
		final var typeId = 20;
		final var typeName = "Bostadsbidrag";
		final var applicantProcessAmount = BigDecimal.valueOf(1850.00);
		final var applicantHandlaggareAmount = BigDecimal.valueOf(1900.00);
		final var applicantAmountDate = now();
		final var coapplicantProcessAmount = BigDecimal.valueOf(950.00);
		final var coapplicantHandlaggareAmount = BigDecimal.valueOf(975.00);
		final var coapplicantAmountDate = now();
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var entity = FaNormIncomeEntity.create()
			.withId(id)
			.withErrandId(errandId)
			.withOrigin(origin)
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withApplicantProcessAmount(applicantProcessAmount)
			.withApplicantHandlaggareAmount(applicantHandlaggareAmount)
			.withApplicantAmountDate(applicantAmountDate)
			.withCoapplicantProcessAmount(coapplicantProcessAmount)
			.withCoapplicantHandlaggareAmount(coapplicantHandlaggareAmount)
			.withCoapplicantAmountDate(coapplicantAmountDate)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getOrigin()).isEqualTo(origin);
		assertThat(entity.getTypeId()).isEqualTo(typeId);
		assertThat(entity.getTypeName()).isEqualTo(typeName);
		assertThat(entity.getApplicantProcessAmount()).isEqualTo(applicantProcessAmount);
		assertThat(entity.getApplicantHandlaggareAmount()).isEqualTo(applicantHandlaggareAmount);
		assertThat(entity.getApplicantAmountDate()).isEqualTo(applicantAmountDate);
		assertThat(entity.getCoapplicantProcessAmount()).isEqualTo(coapplicantProcessAmount);
		assertThat(entity.getCoapplicantHandlaggareAmount()).isEqualTo(coapplicantHandlaggareAmount);
		assertThat(entity.getCoapplicantAmountDate()).isEqualTo(coapplicantAmountDate);
		assertThat(entity.isDeleted()).isEqualTo(deleted);
		assertThat(entity.getNote()).isEqualTo(note);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaNormIncomeEntity.create()).hasAllNullFieldsOrPropertiesExcept("deleted");
		assertThat(new FaNormIncomeEntity()).hasAllNullFieldsOrPropertiesExcept("deleted");
	}

	@Test
	void prePersistAndPreUpdateSetTimestamps() {
		final var entity = FaNormIncomeEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
