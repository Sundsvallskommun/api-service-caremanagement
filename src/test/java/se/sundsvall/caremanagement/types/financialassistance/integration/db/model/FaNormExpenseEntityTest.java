package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaNormExpenseEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaNormExpenseEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("specification", "note"),
			hasValidBeanEqualsExcluding("specification", "note"),
			hasValidBeanToStringExcluding("specification", "note")));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var errandId = "errand";
		final var origin = "SYSTEM";
		final var position = 3;
		final var bucket = "EXPENSE";
		final var costType = "rent";
		final var otherSubType = "other";
		final var specification = "specification";
		final var appliedAmount = BigDecimal.valueOf(1200.00);
		final var processAmount = BigDecimal.valueOf(1000.00);
		final var caseworkerAmount = BigDecimal.valueOf(1100.00);
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var entity = FaNormExpenseEntity.create()
			.withId(id)
			.withErrandId(errandId)
			.withOrigin(origin)
			.withPosition(position)
			.withBucket(bucket)
			.withCostType(costType)
			.withOtherSubType(otherSubType)
			.withSpecification(specification)
			.withAppliedAmount(appliedAmount)
			.withProcessAmount(processAmount)
			.withCaseworkerAmount(caseworkerAmount)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getOrigin()).isEqualTo(origin);
		assertThat(entity.getPosition()).isEqualTo(position);
		assertThat(entity.getBucket()).isEqualTo(bucket);
		assertThat(entity.getCostType()).isEqualTo(costType);
		assertThat(entity.getOtherSubType()).isEqualTo(otherSubType);
		assertThat(entity.getSpecification()).isEqualTo(specification);
		assertThat(entity.getAppliedAmount()).isEqualTo(appliedAmount);
		assertThat(entity.getProcessAmount()).isEqualTo(processAmount);
		assertThat(entity.getCaseworkerAmount()).isEqualTo(caseworkerAmount);
		assertThat(entity.isDeleted()).isEqualTo(deleted);
		assertThat(entity.getNote()).isEqualTo(note);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaNormExpenseEntity.create()).hasAllNullFieldsOrPropertiesExcept("deleted");
		assertThat(new FaNormExpenseEntity()).hasAllNullFieldsOrPropertiesExcept("deleted");
	}

	@Test
	void prePersistAndPreUpdateSetTimestamps() {
		final var entity = FaNormExpenseEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
