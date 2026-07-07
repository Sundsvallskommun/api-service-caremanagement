package se.sundsvall.caremanagement.permit.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermitEntityTest {
	private static final LocalDate FROM = LocalDate.parse("2026-06-03");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");

	@Test
	void builderMethods() {
		final var entity = PermitEntity.create()
			.withId("p1").withErrandId("e1").withPermitType("PARKING_PERMIT").withValidFrom(FROM)
			.withValidUntil(FROM.plusYears(1)).withConditions("c").withStatus("ACTIVE").withCreated(CREATED).withModified(CREATED);

		assertThat(entity.getId()).isEqualTo("p1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(entity.getValidFrom()).isEqualTo(FROM);
		assertThat(entity.getValidUntil()).isEqualTo(FROM.plusYears(1));
		assertThat(entity.getConditions()).isEqualTo("c");
		assertThat(entity.getStatus()).isEqualTo("ACTIVE");
		assertThat(entity.getCreated()).isEqualTo(CREATED);
		assertThat(entity.getModified()).isEqualTo(CREATED);
	}

	@Test
	void setters() {
		final var entity = PermitEntity.create();
		entity.setId("p1");
		entity.setErrandId("e1");
		entity.setPermitType("PARKING_PERMIT");
		entity.setValidFrom(FROM);
		entity.setValidUntil(FROM.plusYears(1));
		entity.setConditions("c");
		entity.setStatus("REVOKED");
		entity.setCreated(CREATED);
		entity.setModified(CREATED);

		assertThat(entity.getId()).isEqualTo("p1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(entity.getValidFrom()).isEqualTo(FROM);
		assertThat(entity.getValidUntil()).isEqualTo(FROM.plusYears(1));
		assertThat(entity.getConditions()).isEqualTo("c");
		assertThat(entity.getStatus()).isEqualTo("REVOKED");
		assertThat(entity.getCreated()).isEqualTo(CREATED);
		assertThat(entity.getModified()).isEqualTo(CREATED);
	}

	@Test
	void toStringContainsValues() {
		final var entity = PermitEntity.create().withId("p1").withErrandId("e1").withStatus("ACTIVE");

		assertThat(entity.toString()).contains("PermitEntity{", "id='p1'", "errandId='e1'", "status='ACTIVE'");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(PermitEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new PermitEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = PermitEntity.create().withId("1").withErrandId("e").withStatus("ACTIVE");
		final var b = PermitEntity.create().withId("1").withErrandId("e").withStatus("ACTIVE");
		final var c = PermitEntity.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
