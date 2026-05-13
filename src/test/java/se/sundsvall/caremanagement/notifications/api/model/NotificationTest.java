package se.sundsvall.caremanagement.notifications.api.model;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NotificationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt(1000)), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Notification.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var errandId = "eid";
		final var ownerId = "jane01doe";
		final var createdBy = "john02doe";
		final var type = "CREATE";
		final var subType = "ERRAND";
		final var description = "desc";
		final var content = "content";
		final var acknowledged = Boolean.TRUE;
		final var expires = OffsetDateTime.now().plusDays(30);
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now();

		final var result = Notification.create()
			.withId(id)
			.withErrandId(errandId)
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

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getErrandId()).isEqualTo(errandId);
		assertThat(result.getOwnerId()).isEqualTo(ownerId);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getSubType()).isEqualTo(subType);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getContent()).isEqualTo(content);
		assertThat(result.getAcknowledged()).isEqualTo(acknowledged);
		assertThat(result.getExpires()).isEqualTo(expires);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Notification.create()).hasAllNullFieldsOrProperties();
	}
}
