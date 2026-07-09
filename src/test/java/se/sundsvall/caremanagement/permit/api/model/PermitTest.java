package se.sundsvall.caremanagement.permit.api.model;

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
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PermitTest {
	private static final LocalDate FROM = LocalDate.parse("2026-06-03");
	private static final LocalDate UNTIL = LocalDate.parse("2031-09-01");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-04T10:00:00Z");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Permit.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var permit = Permit.create()
			.withId("p1").withPermitType("PARKING_PERMIT").withValidFrom(FROM).withValidUntil(UNTIL)
			.withConditions("c").withStatus("ACTIVE").withCreated(CREATED).withModified(MODIFIED);

		assertThat(permit.getId()).isEqualTo("p1");
		assertThat(permit.getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(permit.getValidFrom()).isEqualTo(FROM);
		assertThat(permit.getValidUntil()).isEqualTo(UNTIL);
		assertThat(permit.getConditions()).isEqualTo("c");
		assertThat(permit.getStatus()).isEqualTo("ACTIVE");
		assertThat(permit.getCreated()).isEqualTo(CREATED);
		assertThat(permit.getModified()).isEqualTo(MODIFIED);
	}

	@Test
	void testSetters() {
		final var permit = Permit.create();
		permit.setId("p1");
		permit.setPermitType("PARKING_PERMIT");
		permit.setValidFrom(FROM);
		permit.setValidUntil(UNTIL);
		permit.setConditions("c");
		permit.setStatus("REVOKED");
		permit.setCreated(CREATED);
		permit.setModified(MODIFIED);

		assertThat(permit.getId()).isEqualTo("p1");
		assertThat(permit.getStatus()).isEqualTo("REVOKED");
		assertThat(permit.getValidUntil()).isEqualTo(UNTIL);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Permit.create()).hasAllNullFieldsOrProperties();
		assertThat(new Permit()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsAndHashCode() {
		final var a = Permit.create().withId("1").withPermitType("T").withStatus("ACTIVE");
		final var b = Permit.create().withId("1").withPermitType("T").withStatus("ACTIVE");
		final var c = Permit.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
