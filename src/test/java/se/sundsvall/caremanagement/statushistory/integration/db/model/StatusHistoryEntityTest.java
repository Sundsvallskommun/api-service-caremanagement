package se.sundsvall.caremanagement.statushistory.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class StatusHistoryEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(StatusHistoryEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testToString() {
		final var entity = StatusHistoryEntity.create().withId("id").withErrandId("errand-1");
		assertThat(entity.toString())
			.contains("StatusHistoryEntity{").contains("id='id'").contains("errandId='errand-1'");
	}

	@Test
	void testBuilderMethods() {
		final var changedAt = FIXED_TIMESTAMP;
		final var entity = StatusHistoryEntity.create()
			.withId("id")
			.withErrandId("errand-1")
			.withFromStatus("OPEN")
			.withToStatus("CLOSED")
			.withChangedBy("user")
			.withChangedAt(changedAt);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getFromStatus()).isEqualTo("OPEN");
		assertThat(entity.getToStatus()).isEqualTo("CLOSED");
		assertThat(entity.getChangedBy()).isEqualTo("user");
		assertThat(entity.getChangedAt()).isEqualTo(changedAt);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StatusHistoryEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StatusHistoryEntity()).hasAllNullFieldsOrProperties();
	}
}
