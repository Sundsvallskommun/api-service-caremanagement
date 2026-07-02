package se.sundsvall.caremanagement.eventlog.integration.db.model;

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

class ErrandEventEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(ErrandEventEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testToString() {
		final var entity = ErrandEventEntity.create().withId("id").withErrandId("errand-1");
		assertThat(entity.toString())
			.contains("ErrandEventEntity{").contains("id='id'").contains("errandId='errand-1'");
	}

	@Test
	void testBuilderMethods() {
		final var entity = ErrandEventEntity.create()
			.withId("id")
			.withErrandId("errand-1")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withSource("HTTP")
			.withAction("READ")
			.withTarget("errand")
			.withDescription("READ errand")
			.withHttpMethod("GET")
			.withRequestPath("/2281/FINANCIAL_ASSISTANCE/errands/errand-1")
			.withActor("joe001doe")
			.withActorType("adAccount")
			.withRequestId("req-1")
			.withStatusCode(200)
			.withCreated(FIXED_TIMESTAMP);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(entity.getSource()).isEqualTo("HTTP");
		assertThat(entity.getAction()).isEqualTo("READ");
		assertThat(entity.getTarget()).isEqualTo("errand");
		assertThat(entity.getDescription()).isEqualTo("READ errand");
		assertThat(entity.getHttpMethod()).isEqualTo("GET");
		assertThat(entity.getRequestPath()).isEqualTo("/2281/FINANCIAL_ASSISTANCE/errands/errand-1");
		assertThat(entity.getActor()).isEqualTo("joe001doe");
		assertThat(entity.getActorType()).isEqualTo("adAccount");
		assertThat(entity.getRequestId()).isEqualTo("req-1");
		assertThat(entity.getStatusCode()).isEqualTo(200);
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandEventEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandEventEntity()).hasAllNullFieldsOrProperties();
	}
}
