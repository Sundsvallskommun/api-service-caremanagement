package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
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
import static java.time.Month.JULY;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class MonitoringTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Monitoring.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var created = OffsetDateTime.parse("2026-06-01T12:00:00Z");
		final var startDate = LocalDate.of(2026, JULY, 1);
		final var endDate = LocalDate.of(2026, JULY, 31);
		final var monitoring = Monitoring.create()
			.withId("id")
			.withSource("LIFECARE")
			.withLifecareId("987654")
			.withTitle("Följ upp")
			.withDescription("Inväntar underlag")
			.withStartDate(startDate)
			.withEndDate(endDate)
			.withCreatedBy("joe01doe")
			.withCreated(created)
			.withUpdated(created);

		assertThat(monitoring.getId()).isEqualTo("id");
		assertThat(monitoring.getSource()).isEqualTo("LIFECARE");
		assertThat(monitoring.getLifecareId()).isEqualTo("987654");
		assertThat(monitoring.getTitle()).isEqualTo("Följ upp");
		assertThat(monitoring.getStartDate()).isEqualTo(startDate);
		assertThat(monitoring.getEndDate()).isEqualTo(endDate);
		assertThat(monitoring.getCreatedBy()).isEqualTo("joe01doe");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Monitoring.create()).hasAllNullFieldsOrProperties();
	}
}
