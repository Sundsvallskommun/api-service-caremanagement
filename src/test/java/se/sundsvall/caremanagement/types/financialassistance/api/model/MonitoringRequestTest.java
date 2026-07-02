package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class MonitoringRequestTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(MonitoringRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var startDate = LocalDate.of(2026, JULY, 1);
		final var endDate = LocalDate.of(2026, JULY, 31);
		final var request = MonitoringRequest.create()
			.withSource("LIFECARE")
			.withLifecareId("987654")
			.withTitle("Följ upp")
			.withDescription("Inväntar underlag")
			.withStartDate(startDate)
			.withEndDate(endDate)
			.withCreatedBy("joe01doe");

		assertThat(request).hasNoNullFieldsOrProperties();
		assertThat(request.getSource()).isEqualTo("LIFECARE");
		assertThat(request.getLifecareId()).isEqualTo("987654");
		assertThat(request.getTitle()).isEqualTo("Följ upp");
		assertThat(request.getStartDate()).isEqualTo(startDate);
		assertThat(request.getEndDate()).isEqualTo(endDate);
		assertThat(request.getCreatedBy()).isEqualTo("joe01doe");
	}

	@Test
	void createReturnsEmptyInstance() {
		assertThat(MonitoringRequest.create()).hasAllNullFieldsOrProperties();
	}
}
