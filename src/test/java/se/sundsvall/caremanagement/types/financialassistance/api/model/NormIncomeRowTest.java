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

class NormIncomeRowTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NormIncomeRow.class, allOf(
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
		final var typeId = 20;
		final var typeName = "Bostadsbidrag";
		final var recipient = "APPLICANT";
		final var processAmount = BigDecimal.valueOf(1850.00);
		final var processAmountDate = now();
		final var handlaggareAmount = BigDecimal.valueOf(1900.00);
		final var handlaggareAmountDate = now();
		final var effectiveAmount = BigDecimal.valueOf(1900.00);
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var result = NormIncomeRow.create()
			.withId(id)
			.withOrigin(origin)
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withRecipient(recipient)
			.withProcessAmount(processAmount)
			.withProcessAmountDate(processAmountDate)
			.withHandlaggareAmount(handlaggareAmount)
			.withHandlaggareAmountDate(handlaggareAmountDate)
			.withEffectiveAmount(effectiveAmount)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getOrigin()).isEqualTo(origin);
		assertThat(result.getTypeId()).isEqualTo(typeId);
		assertThat(result.getTypeName()).isEqualTo(typeName);
		assertThat(result.getRecipient()).isEqualTo(recipient);
		assertThat(result.getProcessAmount()).isEqualTo(processAmount);
		assertThat(result.getProcessAmountDate()).isEqualTo(processAmountDate);
		assertThat(result.getHandlaggareAmount()).isEqualTo(handlaggareAmount);
		assertThat(result.getHandlaggareAmountDate()).isEqualTo(handlaggareAmountDate);
		assertThat(result.getEffectiveAmount()).isEqualTo(effectiveAmount);
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
