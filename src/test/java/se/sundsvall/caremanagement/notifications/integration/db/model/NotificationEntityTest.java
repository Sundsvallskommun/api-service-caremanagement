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
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testBuilderMethods() {
		final var expires = now().plusDays(10);
		final var created = now();
		final var modified = now();

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

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getErrandId()).isEqualTo("errand");
		org.assertj.core.api.Assertions.assertThat(entity.getMunicipalityId()).isEqualTo("mid");
		org.assertj.core.api.Assertions.assertThat(entity.getNamespace()).isEqualTo("ns");
		org.assertj.core.api.Assertions.assertThat(entity.getOwnerId()).isEqualTo("owner");
		org.assertj.core.api.Assertions.assertThat(entity.getCreatedBy()).isEqualTo("creator");
		org.assertj.core.api.Assertions.assertThat(entity.getType()).isEqualTo(NotificationType.CREATE);
		org.assertj.core.api.Assertions.assertThat(entity.getSubType()).isEqualTo(NotificationSubType.ERRAND);
		org.assertj.core.api.Assertions.assertThat(entity.getDescription()).isEqualTo("desc");
		org.assertj.core.api.Assertions.assertThat(entity.getContent()).isEqualTo("content");
		org.assertj.core.api.Assertions.assertThat(entity.isAcknowledged()).isTrue();
		org.assertj.core.api.Assertions.assertThat(entity.getExpires()).isEqualTo(expires);
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isEqualTo(created);
		org.assertj.core.api.Assertions.assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		final var entity = NotificationEntity.create();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isNull();
		org.assertj.core.api.Assertions.assertThat(entity.isAcknowledged()).isFalse();
	}

	@Test
	void testToString() {
		final var entity = NotificationEntity.create().withId("id").withErrandId("e").withOwnerId("o").withDescription("d");
		org.assertj.core.api.Assertions.assertThat(entity.toString())
			.contains("NotificationEntity{").contains("id='id'").contains("errandId='e'").contains("ownerId='o'");
	}
}
