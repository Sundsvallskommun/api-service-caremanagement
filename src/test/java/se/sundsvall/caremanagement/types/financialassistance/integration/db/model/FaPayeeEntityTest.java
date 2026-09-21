package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaPayeeEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaPayeeEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-09-21T12:00:00Z");
		final var modified = OffsetDateTime.parse("2026-09-21T13:00:00Z");

		final var entity = FaPayeeEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withName("Sundsvalls Hyresbostäder AB")
			.withPaymentMethod("Bankgiro via Plusgiro")
			.withClearing("6000")
			.withAccountNumber("5555-6666")
			.withLifecareStatus("PENDING")
			.withLifecarePayeeId("44213")
			.withLifecareDetail("Kontonummer har fel format")
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getName()).isEqualTo("Sundsvalls Hyresbostäder AB");
		assertThat(entity.getPaymentMethod()).isEqualTo("Bankgiro via Plusgiro");
		assertThat(entity.getClearing()).isEqualTo("6000");
		assertThat(entity.getAccountNumber()).isEqualTo("5555-6666");
		assertThat(entity.getLifecareStatus()).isEqualTo("PENDING");
		assertThat(entity.getLifecarePayeeId()).isEqualTo("44213");
		assertThat(entity.getLifecareDetail()).isEqualTo("Kontonummer har fel format");
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaPayeeEntity.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsCreated() {
		final var entity = FaPayeeEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void preUpdateSetsModified() {
		final var entity = FaPayeeEntity.create();
		entity.preUpdate();
		assertThat(entity.getModified()).isNotNull();
	}
}
