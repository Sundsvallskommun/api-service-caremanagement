package se.sundsvall.caremanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ContactReasonEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ContactReasonEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var id = 1L;
		final var modified = OffsetDateTime.now();
		final var municipalityId = "municipalityId";
		final var name = "NEW_APPLICATION";
		final var displayName = "Ny ansökan";
		final var namespace = "namespace";

		final var entity = ContactReasonEntity.create()
			.withCreated(created)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withName(name)
			.withDisplayName(displayName)
			.withNamespace(namespace);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
	}

	@Test
	void testOnCreate() {
		final var entity = ContactReasonEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = ContactReasonEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ContactReasonEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ContactReasonEntity()).hasAllNullFieldsOrProperties();
	}
}
