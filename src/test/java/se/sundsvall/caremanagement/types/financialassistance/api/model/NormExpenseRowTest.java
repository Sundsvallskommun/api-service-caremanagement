package se.sundsvall.caremanagement.types.financialassistance.api.model;

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

class NormExpenseRowTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NormExpenseRow.class, allOf(
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
		final var costType = "rent";
		final var otherSubType = "other";
		final var specification = "specification";
		final var appliedAmount = BigDecimal.valueOf(1200.00);
		final var processAmount = BigDecimal.valueOf(1000.00);
		final var handlaggareAmount = BigDecimal.valueOf(1100.00);
		final var effectiveAmount = BigDecimal.valueOf(1100.00);
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var result = NormExpenseRow.create()
			.withId(id)
			.withOrigin(origin)
			.withCostType(costType)
			.withOtherSubType(otherSubType)
			.withSpecification(specification)
			.withAppliedAmount(appliedAmount)
			.withProcessAmount(processAmount)
			.withHandlaggareAmount(handlaggareAmount)
			.withEffectiveAmount(effectiveAmount)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getOrigin()).isEqualTo(origin);
		assertThat(result.getCostType()).isEqualTo(costType);
		assertThat(result.getOtherSubType()).isEqualTo(otherSubType);
		assertThat(result.getSpecification()).isEqualTo(specification);
		assertThat(result.getAppliedAmount()).isEqualTo(appliedAmount);
		assertThat(result.getProcessAmount()).isEqualTo(processAmount);
		assertThat(result.getHandlaggareAmount()).isEqualTo(handlaggareAmount);
		assertThat(result.getEffectiveAmount()).isEqualTo(effectiveAmount);
		assertThat(result.isDeleted()).isEqualTo(deleted);
		assertThat(result.getNote()).isEqualTo(note);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormExpenseRow.create()).hasAllNullFieldsOrPropertiesExcept("deleted");
		assertThat(new NormExpenseRow()).hasAllNullFieldsOrPropertiesExcept("deleted");
	}
}
