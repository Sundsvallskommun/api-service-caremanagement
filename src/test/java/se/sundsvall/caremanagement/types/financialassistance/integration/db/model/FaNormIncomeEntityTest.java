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
		final var recipient = "APPLICANT";
		final var processAmount = BigDecimal.valueOf(1850.00);
		final var processAmountDate = now();
		final var handlaggareAmount = BigDecimal.valueOf(1900.00);
		final var handlaggareAmountDate = now();
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
			.withRecipient(recipient)
			.withProcessAmount(processAmount)
			.withProcessAmountDate(processAmountDate)
			.withHandlaggareAmount(handlaggareAmount)
			.withHandlaggareAmountDate(handlaggareAmountDate)
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
		assertThat(entity.getRecipient()).isEqualTo(recipient);
		assertThat(entity.getProcessAmount()).isEqualTo(processAmount);
		assertThat(entity.getProcessAmountDate()).isEqualTo(processAmountDate);
		assertThat(entity.getHandlaggareAmount()).isEqualTo(handlaggareAmount);
		assertThat(entity.getHandlaggareAmountDate()).isEqualTo(handlaggareAmountDate);
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
