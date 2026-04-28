package se.sundsvall.caremanagement.api.model;

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

class DecisionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt(1000)), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Decision.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var decisionType = "PAYMENT";
		final var value = "APPROVED";
		final var description = "desc";
		final var createdBy = "jane01doe";
		final var created = OffsetDateTime.now();

		final var result = Decision.create()
			.withId(id)
			.withDecisionType(decisionType)
			.withValue(value)
			.withDescription(description)
			.withCreatedBy(createdBy)
			.withCreated(created);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getDecisionType()).isEqualTo(decisionType);
		assertThat(result.getValue()).isEqualTo(value);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
		assertThat(result.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Decision.create()).hasAllNullFieldsOrProperties();
	}
}
