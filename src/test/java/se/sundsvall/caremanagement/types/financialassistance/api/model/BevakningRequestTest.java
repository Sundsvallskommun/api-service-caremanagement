package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class BevakningRequestTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1_000_000)), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(BevakningRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var startDate = LocalDate.of(2026, 7, 1);
		final var endDate = LocalDate.of(2026, 7, 31);
		final var request = BevakningRequest.create()
			.withTitle("Följ upp")
			.withDescription("Inväntar underlag")
			.withStartDate(startDate)
			.withEndDate(endDate)
			.withCreatedBy("joe01doe");

		org.assertj.core.api.Assertions.assertThat(request).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(request.getTitle()).isEqualTo("Följ upp");
		org.assertj.core.api.Assertions.assertThat(request.getStartDate()).isEqualTo(startDate);
		org.assertj.core.api.Assertions.assertThat(request.getEndDate()).isEqualTo(endDate);
		org.assertj.core.api.Assertions.assertThat(request.getCreatedBy()).isEqualTo("joe01doe");
	}

	@Test
	void createReturnsEmptyInstance() {
		org.assertj.core.api.Assertions.assertThat(BevakningRequest.create()).hasAllNullFieldsOrProperties();
	}
}
