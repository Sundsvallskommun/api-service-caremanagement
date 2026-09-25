package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
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

class FaCalculationSyncEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaCalculationSyncEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var timestamp = OffsetDateTime.parse("2026-09-25T03:00:00Z");
		final var entity = FaCalculationSyncEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withIncomeTypeKey("lön")
			.withIncomeTypeId(20)
			.withIncomeTypeName("Lön")
			.withRole("APPLICANT")
			.withSsbtekAmount(new BigDecimal("12400"))
			.withSsbtekBaselineAmount(new BigDecimal("11900"))
			.withSsbtekReadAt(timestamp)
			.withSystemWrittenAmount(new BigDecimal("11900"))
			.withSystemWrittenAt(timestamp)
			.withCreated(timestamp)
			.withUpdated(timestamp);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getIncomeTypeKey()).isEqualTo("lön");
		assertThat(entity.getIncomeTypeId()).isEqualTo(20);
		assertThat(entity.getIncomeTypeName()).isEqualTo("Lön");
		assertThat(entity.getRole()).isEqualTo("APPLICANT");
		assertThat(entity.getSsbtekAmount()).isEqualByComparingTo("12400");
		assertThat(entity.getSsbtekBaselineAmount()).isEqualByComparingTo("11900");
		assertThat(entity.getSsbtekReadAt()).isEqualTo(timestamp);
		assertThat(entity.getSystemWrittenAmount()).isEqualByComparingTo("11900");
		assertThat(entity.getSystemWrittenAt()).isEqualTo(timestamp);
		assertThat(entity.getCreated()).isEqualTo(timestamp);
		assertThat(entity.getUpdated()).isEqualTo(timestamp);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaCalculationSyncEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FaCalculationSyncEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void prePersistSetsTimestamps() {
		final var entity = FaCalculationSyncEntity.create();
		entity.prePersist();
		assertThat(entity.getCreated()).isNotNull();
		assertThat(entity.getUpdated()).isNotNull();
	}

	@Test
	void preUpdateSetsUpdated() {
		final var entity = FaCalculationSyncEntity.create();
		entity.preUpdate();
		assertThat(entity.getUpdated()).isNotNull();
	}
}
