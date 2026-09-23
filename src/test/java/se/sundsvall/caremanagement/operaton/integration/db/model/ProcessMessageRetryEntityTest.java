package se.sundsvall.caremanagement.operaton.integration.db.model;

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

class ProcessMessageRetryEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(ProcessMessageRetryEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var entity = ProcessMessageRetryEntity.create()
			.withId("r1")
			.withErrandId("e1")
			.withMunicipalityId("2281")
			.withNamespace("MY_NAMESPACE")
			.withMessageName("PaymentDecisionReceived")
			.withVariables("{}")
			.withStatus("PENDING")
			.withAttempts(2)
			.withLastError("engine down")
			.withNextAttempt(FIXED_TIMESTAMP)
			.withCreated(FIXED_TIMESTAMP);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("r1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("MY_NAMESPACE");
		assertThat(entity.getMessageName()).isEqualTo("PaymentDecisionReceived");
		assertThat(entity.getVariables()).isEqualTo("{}");
		assertThat(entity.getStatus()).isEqualTo("PENDING");
		assertThat(entity.getAttempts()).isEqualTo(2);
		assertThat(entity.getLastError()).isEqualTo("engine down");
		assertThat(entity.getNextAttempt()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessMessageRetryEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ProcessMessageRetryEntity()).hasAllNullFieldsOrProperties();
	}
}
