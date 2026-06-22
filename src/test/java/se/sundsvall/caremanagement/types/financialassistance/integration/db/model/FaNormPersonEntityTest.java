package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.LocalDate;
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

class FaNormPersonEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(FaNormPersonEntity.class, allOf(
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
		final var partyId = "partyId";
		final var role = "APPLICANT";
		final var name = "name";
		final var processDays = 30;
		final var caseworkerDays = 15;
		final var included = true;
		final var deviationFromDate = LocalDate.of(2026, 6, 1);
		final var deviationToDate = LocalDate.of(2026, 6, 15);
		final var normInterval = "MONTH";
		final var jobStimulusAmount = BigDecimal.valueOf(1000.00);
		final var deleted = true;
		final var note = "note";
		final var created = now();
		final var updated = now();

		final var entity = FaNormPersonEntity.create()
			.withId(id)
			.withErrandId(errandId)
			.withOrigin(origin)
			.withPartyId(partyId)
			.withRole(role)
			.withName(name)
			.withProcessDays(processDays)
			.withCaseworkerDays(caseworkerDays)
			.withIncluded(included)
			.withDeviationFromDate(deviationFromDate)
			.withDeviationToDate(deviationToDate)
			.withNormInterval(normInterval)
			.withJobStimulusAmount(jobStimulusAmount)
			.withDeleted(deleted)
			.withNote(note)
			.withCreated(created)
			.withUpdated(updated);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getOrigin()).isEqualTo(origin);
		assertThat(entity.getPartyId()).isEqualTo(partyId);
		assertThat(entity.getRole()).isEqualTo(role);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getProcessDays()).isEqualTo(processDays);
		assertThat(entity.getCaseworkerDays()).isEqualTo(caseworkerDays);
		assertThat(entity.isIncluded()).isEqualTo(included);
		assertThat(entity.getDeviationFromDate()).isEqualTo(deviationFromDate);
		assertThat(entity.getDeviationToDate()).isEqualTo(deviationToDate);
		assertThat(entity.getNormInterval()).isEqualTo(normInterval);
		assertThat(entity.getJobStimulusAmount()).isEqualTo(jobStimulusAmount);
		assertThat(entity.isDeleted()).isEqualTo(deleted);
		assertThat(entity.getNote()).isEqualTo(note);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getUpdated()).isEqualTo(updated);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaNormPersonEntity.create()).hasAllNullFieldsOrPropertiesExcept("deleted", "included");
		assertThat(new FaNormPersonEntity()).hasAllNullFieldsOrPropertiesExcept("deleted", "included");
	}

	@Test
	void prePersistAndPreUpdateSetTimestamps() {
		final var entity = FaNormPersonEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
