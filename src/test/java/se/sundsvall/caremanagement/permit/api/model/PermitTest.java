package se.sundsvall.caremanagement.permit.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermitTest {
	private static final LocalDate FROM = LocalDate.parse("2026-06-03");
	private static final LocalDate UNTIL = LocalDate.parse("2031-09-01");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-04T10:00:00Z");

	@Test
	void builderMethods() {
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
	void setters() {
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
	void createReturnsBlankInstance() {
		assertThat(Permit.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = Permit.create().withId("1").withPermitType("T").withStatus("ACTIVE");
		final var b = Permit.create().withId("1").withPermitType("T").withStatus("ACTIVE");
		final var c = Permit.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
