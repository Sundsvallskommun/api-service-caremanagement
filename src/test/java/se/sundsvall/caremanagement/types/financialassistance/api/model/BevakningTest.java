package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
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
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class BevakningTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(Bevakning.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var startDate = LocalDate.of(2026, 7, 1);
		final var endDate = LocalDate.of(2026, 7, 31);
		final var bevakning = Bevakning.create()
			.withId("id")
			.withTitle("Följ upp")
			.withDescription("Inväntar underlag")
			.withStartDate(startDate)
			.withEndDate(endDate)
			.withCreatedBy("joe01doe")
			.withCreated(created)
			.withUpdated(created);

		org.assertj.core.api.Assertions.assertThat(bevakning.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(bevakning.getTitle()).isEqualTo("Följ upp");
		org.assertj.core.api.Assertions.assertThat(bevakning.getStartDate()).isEqualTo(startDate);
		org.assertj.core.api.Assertions.assertThat(bevakning.getEndDate()).isEqualTo(endDate);
		org.assertj.core.api.Assertions.assertThat(bevakning.getCreatedBy()).isEqualTo("joe01doe");
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(Bevakning.create()).hasAllNullFieldsOrProperties();
	}
}
