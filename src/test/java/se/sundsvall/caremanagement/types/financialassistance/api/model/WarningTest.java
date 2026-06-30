package se.sundsvall.caremanagement.types.financialassistance.api.model;

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
import static org.hamcrest.core.AllOf.allOf;

class WarningTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Warning.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var warning = Warning.create()
			.withId("id")
			.withType("MISSING_SSBTEK")
			.withSourceKey("Dagersättning")
			.withMessage("Still missing in SSBTEK: Dagersättning")
			.withStatus("OPEN")
			.withAutoResolved(false)
			.withCreated(created)
			.withUpdated(created);

		assertThat(warning.getId()).isEqualTo("id");
		assertThat(warning.getType()).isEqualTo("MISSING_SSBTEK");
		assertThat(warning.getStatus()).isEqualTo("OPEN");
		assertThat(warning.getMessage()).isEqualTo("Still missing in SSBTEK: Dagersättning");
	}

	@Test
	void createReturnsEmptyInstance() {
		assertThat(Warning.create()).hasAllNullFieldsOrPropertiesExcept("autoResolved");
	}
}
