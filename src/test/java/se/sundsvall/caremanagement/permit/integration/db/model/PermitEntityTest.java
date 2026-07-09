package se.sundsvall.caremanagement.permit.integration.db.model;

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
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class PermitEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(PermitEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var from = LocalDate.parse("2026-06-03");
		final var created = OffsetDateTime.parse("2026-06-03T10:00:00Z");

		final var entity = PermitEntity.create()
			.withId("p1").withErrandId("e1").withPermitType("PARKING_PERMIT").withValidFrom(from)
			.withValidUntil(from.plusYears(1)).withConditions("c").withStatus("ACTIVE").withCreated(created).withModified(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("p1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(entity.getValidFrom()).isEqualTo(from);
		assertThat(entity.getValidUntil()).isEqualTo(from.plusYears(1));
		assertThat(entity.getConditions()).isEqualTo("c");
		assertThat(entity.getStatus()).isEqualTo("ACTIVE");
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(created);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(PermitEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new PermitEntity()).hasAllNullFieldsOrProperties();
	}
}
