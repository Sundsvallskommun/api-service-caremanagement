package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
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

class NormPersonRowTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NormPersonRow.class, allOf(
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
		final var partyId = "partyId";
		final var role = "APPLICANT";
		final var name = "name";
		final var processDays = 30;
		final var handlaggareDays = 15;
		final var effectiveDays = 15;
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var result = NormPersonRow.create()
			.withId(id)
			.withOrigin(origin)
			.withPartyId(partyId)
			.withRole(role)
			.withName(name)
			.withProcessDays(processDays)
			.withHandlaggareDays(handlaggareDays)
			.withEffectiveDays(effectiveDays)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getOrigin()).isEqualTo(origin);
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getRole()).isEqualTo(role);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getProcessDays()).isEqualTo(processDays);
		assertThat(result.getHandlaggareDays()).isEqualTo(handlaggareDays);
		assertThat(result.getEffectiveDays()).isEqualTo(effectiveDays);
		assertThat(result.isDeleted()).isEqualTo(deleted);
		assertThat(result.getNote()).isEqualTo(note);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormPersonRow.create()).hasAllNullFieldsOrPropertiesExcept("deleted");
		assertThat(new NormPersonRow()).hasAllNullFieldsOrPropertiesExcept("deleted");
	}
}
