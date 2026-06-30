package se.sundsvall.caremanagement.notifications.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NotificationEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(NotificationEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testBuilderMethods() {
		final var expires = FIXED_TIMESTAMP.plusDays(10);
		final var created = FIXED_TIMESTAMP;
		final var modified = FIXED_TIMESTAMP;

		final var entity = NotificationEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withMunicipalityId("mid")
			.withNamespace("ns")
			.withOwnerId("owner")
			.withCreatedBy("creator")
			.withType(NotificationType.CREATE)
			.withSubType(NotificationSubType.ERRAND)
			.withDescription("desc")
			.withContent("content")
			.withAcknowledged(true)
			.withExpires(expires)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getMunicipalityId()).isEqualTo("mid");
		assertThat(entity.getNamespace()).isEqualTo("ns");
		assertThat(entity.getOwnerId()).isEqualTo("owner");
		assertThat(entity.getCreatedBy()).isEqualTo("creator");
		assertThat(entity.getType()).isEqualTo(NotificationType.CREATE);
		assertThat(entity.getSubType()).isEqualTo(NotificationSubType.ERRAND);
		assertThat(entity.getDescription()).isEqualTo("desc");
		assertThat(entity.getContent()).isEqualTo("content");
		assertThat(entity.isAcknowledged()).isTrue();
		assertThat(entity.getExpires()).isEqualTo(expires);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		final var entity = NotificationEntity.create();
		assertThat(entity.getId()).isNull();
		assertThat(entity.isAcknowledged()).isFalse();
	}

	@Test
	void testToString() {
		final var entity = NotificationEntity.create().withId("id").withErrandId("e").withOwnerId("o").withDescription("d");
		assertThat(entity.toString())
			.contains("NotificationEntity{").contains("id='id'").contains("errandId='e'").contains("ownerId='o'");
	}
}
