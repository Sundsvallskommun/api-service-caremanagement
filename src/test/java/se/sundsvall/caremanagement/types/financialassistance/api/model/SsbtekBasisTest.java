package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.Month.JULY;
import static java.time.Month.SEPTEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class SsbtekBasisTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(365)), LocalDate.class);
	}

	/**
	 * {@code hasValidBeanToString()} is deliberately absent: it asserts that every property value appears in
	 * {@code toString()}, which is the exact opposite of what this bean must do with its agency payloads. The redaction
	 * is asserted by {@link #testToStringRedactsAgencyPayload()} instead.
	 */
	@Test
	void testBean() {
		MatcherAssert.assertThat(SsbtekBasis.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testBuilderMethods() {
		final var agencies = Map.<String, Map<String, Object>>of("fk", Map.of("formansinformation", Map.of()));

		final var basis = SsbtekBasis.create()
			.withFrom(LocalDate.of(2026, JULY, 1))
			.withTo(LocalDate.of(2026, SEPTEMBER, 30))
			.withAgencies(agencies);

		assertThat(basis.getFrom()).isEqualTo(LocalDate.of(2026, JULY, 1));
		assertThat(basis.getTo()).isEqualTo(LocalDate.of(2026, SEPTEMBER, 30));
		assertThat(basis.getAgencies()).isEqualTo(agencies);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SsbtekBasis.create()).hasAllNullFieldsOrProperties();
		assertThat(new SsbtekBasis()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testToStringRedactsAgencyPayload() {
		final var basis = SsbtekBasis.create()
			.withFrom(LocalDate.of(2026, JULY, 1))
			.withTo(LocalDate.of(2026, SEPTEMBER, 30))
			.withAgencies(Map.of("fk", Map.of("nettobelopp", "12345")));

		assertThat(basis.toString())
			.contains("2026-07-01", "2026-09-30", "fk")
			.doesNotContain("nettobelopp", "12345");
	}

	@Test
	void testToStringHandlesMissingAgencies() {
		assertThat(SsbtekBasis.create().toString()).contains("agencies=null");
	}
}
