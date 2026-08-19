package se.sundsvall.caremanagement.core.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
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

class ErrandEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(ErrandEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("description"),
			hasValidBeanEqualsExcluding("description"),
			hasValidBeanToStringExcluding("description")));
	}

	@Test
	void testBuilderMethods() {
		final var now = FIXED_TIMESTAMP;

		final var id = UUID.randomUUID().toString();
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var errandNumber = "CAREM-2026-00042";
		final var typeSlug = "typeSlug";
		final var title = "title";
		final var status = "status";
		final var description = "description";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var assignedUserId = "assignedUserId";
		final var applicantName = "Anna Andersson";
		final var processDefinitionName = "Handläggning";
		final var processInstanceId = "pi-1";

		final var entity = ErrandEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandNumber(errandNumber)
			.withTypeSlug(typeSlug)
			.withTitle(title)
			.withStatus(status)
			.withDescription(description)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withAssignedUserId(assignedUserId)
			.withApplicantName(applicantName)
			.withProcessDefinitionName(processDefinitionName)
			.withProcessInstanceId(processInstanceId)
			.withCreated(now)
			.withModified(now)
			.withTouched(now);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(entity.getTypeSlug()).isEqualTo(typeSlug);
		assertThat(entity.getTitle()).isEqualTo(title);
		assertThat(entity.getStatus()).isEqualTo(status);
		assertThat(entity.getDescription()).isEqualTo(description);
		assertThat(entity.getPriority()).isEqualTo(priority);
		assertThat(entity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(entity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(entity.getApplicantName()).isEqualTo(applicantName);
		assertThat(entity.getProcessDefinitionName()).isEqualTo(processDefinitionName);
		assertThat(entity.getProcessInstanceId()).isEqualTo(processInstanceId);
		assertThat(entity).extracting(
			ErrandEntity::getCreated,
			ErrandEntity::getModified,
			ErrandEntity::getTouched).allSatisfy(date -> assertThat(date).isEqualTo(now));
	}

	@Test
	void getTouchedFallsBackToModifiedWhenNullAndModifiedAfterCreated() {
		final var created = FIXED_TIMESTAMP.minusDays(1);
		final var modified = FIXED_TIMESTAMP;

		final var entity = ErrandEntity.create()
			.withCreated(created)
			.withModified(modified);

		assertThat(entity.getTouched()).isEqualTo(modified);
	}

	@Test
	void getTouchedFallsBackToCreatedWhenModifiedIsNull() {
		final var created = FIXED_TIMESTAMP.minusDays(1);

		final var entity = ErrandEntity.create().withCreated(created);

		assertThat(entity.getTouched()).isEqualTo(created);
	}

	@Test
	void getTouchedReturnsExplicitTouchedWhenSet() {
		final var touched = FIXED_TIMESTAMP;

		final var entity = ErrandEntity.create().withTouched(touched);

		assertThat(entity.getTouched()).isEqualTo(touched);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void onCreateOrUpdateSetsTouchedAndLeavesErrandNumberToTheService() {
		final var entity = new ErrandEntity().withStatus("status");

		entity.onCreateOrUpdate();

		assertThat(entity.getTouched()).isNotNull();
		assertThat(entity.getErrandNumber()).isNull();
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("touched", "status");
	}
}
