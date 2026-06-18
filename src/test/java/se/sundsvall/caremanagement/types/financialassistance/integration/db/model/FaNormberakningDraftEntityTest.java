package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
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

class FaNormberakningDraftEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(FaNormberakningDraftEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var entity = FaNormberakningDraftEntity.create()
			.withErrandId("errand")
			.withApplicationMonth("2026-06")
			.withNormId(7)
			.withNormType("RIKSNORM")
			.withCreated(created)
			.withUpdated(created);

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getErrandId()).isEqualTo("errand");
		org.assertj.core.api.Assertions.assertThat(entity.getNormId()).isEqualTo(7);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(FaNormberakningDraftEntity.create()).hasAllNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(new FaNormberakningDraftEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistAndPreUpdateSetTimestamps() {
		final var entity = FaNormberakningDraftEntity.create();
		entity.prePersist();
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isNotNull();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
		entity.preUpdate();
		org.assertj.core.api.Assertions.assertThat(entity.getUpdated()).isNotNull();
	}
}
