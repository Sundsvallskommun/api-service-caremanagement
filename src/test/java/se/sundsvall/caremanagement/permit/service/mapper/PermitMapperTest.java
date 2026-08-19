package se.sundsvall.caremanagement.permit.service.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.permit.api.model.Permit;
import se.sundsvall.caremanagement.permit.integration.db.model.PermitEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PermitMapperTest {

	@Test
	void toPermitMapsAllFields() {
		final var entity = PermitEntity.create()
			.withId("p1").withPermitType("PARKING_PERMIT").withValidFrom(LocalDate.parse("2026-06-03"))
			.withValidUntil(LocalDate.parse("2031-09-01")).withConditions("c").withStatus("ACTIVE")
			.withCreated(OffsetDateTime.parse("2026-06-03T10:00:00Z")).withModified(OffsetDateTime.parse("2026-06-04T10:00:00Z"));

		final var permit = PermitMapper.toPermit(entity);

		assertThat(permit).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(permit.getId()).isEqualTo("p1");
		assertThat(permit.getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(permit.getValidFrom()).isEqualTo(LocalDate.parse("2026-06-03"));
		assertThat(permit.getValidUntil()).isEqualTo(LocalDate.parse("2031-09-01"));
		assertThat(permit.getConditions()).isEqualTo("c");
		assertThat(permit.getStatus()).isEqualTo("ACTIVE");
		assertThat(permit.getCreated()).isEqualTo(OffsetDateTime.parse("2026-06-03T10:00:00Z"));
		assertThat(permit.getModified()).isEqualTo(OffsetDateTime.parse("2026-06-04T10:00:00Z"));
	}

	@Test
	void toPermitEntityMapsFieldsAndErrandId() {
		final var entity = PermitMapper.toPermitEntity(
			Permit.create().withPermitType("PARKING_PERMIT").withConditions("c").withStatus("ACTIVE"), "errand-1");

		assertThat(entity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "validFrom", "validUntil", "created", "modified");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(entity.getConditions()).isEqualTo("c");
		assertThat(entity.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	void nullSafe() {
		assertThat(PermitMapper.toPermit(null)).isNull();
		assertThat(PermitMapper.toPermitEntity(null, "errand-1")).isNull();
		assertThat(PermitMapper.toPermitList(null)).isEmpty();
	}

	@Test
	void toPermitListMapsEach() {
		final var permits = PermitMapper.toPermitList(List.of(PermitEntity.create().withId("p1"), PermitEntity.create().withId("p2")));

		assertThat(permits).extracting(Permit::getId).containsExactly("p1", "p2");
	}
}
