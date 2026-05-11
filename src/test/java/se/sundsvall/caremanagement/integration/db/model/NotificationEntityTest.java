package se.sundsvall.caremanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NotificationEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NotificationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create().withId("eid");
		final var municipalityId = "2281";
		final var namespace = "ns";
		final var ownerId = "jane01doe";
		final var createdBy = "john02doe";
		final var type = NotificationType.CREATE;
		final var subType = NotificationSubType.ERRAND;
		final var description = "desc";
		final var content = "content";
		final var acknowledged = true;
		final var expires = now().plusDays(30);
		final var created = now();
		final var modified = now();

		final var entity = NotificationEntity.create()
			.withId(id)
			.withErrandEntity(errand)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withOwnerId(ownerId)
			.withCreatedBy(createdBy)
			.withType(type)
			.withSubType(subType)
			.withDescription(description)
			.withContent(content)
			.withAcknowledged(acknowledged)
			.withExpires(expires)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandEntity()).isEqualTo(errand);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getOwnerId()).isEqualTo(ownerId);
		assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
		assertThat(entity.getType()).isEqualTo(type);
		assertThat(entity.getSubType()).isEqualTo(subType);
		assertThat(entity.getDescription()).isEqualTo(description);
		assertThat(entity.getContent()).isEqualTo(content);
		assertThat(entity.isAcknowledged()).isEqualTo(acknowledged);
		assertThat(entity.getExpires()).isEqualTo(expires);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NotificationEntity.create()).hasAllNullFieldsOrPropertiesExcept("acknowledged");
		assertThat(NotificationEntity.create().isAcknowledged()).isFalse();
	}

	@Test
	void toStringExcludesErrandEntity() {
		assertThat(NotificationEntity.create().toString()).doesNotContain("errandEntity");
	}
}
