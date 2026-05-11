package se.sundsvall.caremanagement.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.Notification;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationSubType;
import se.sundsvall.caremanagement.integration.db.model.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

	@Test
	void toEntity_maps() {
		final var errand = ErrandEntity.create().withId("eid").withMunicipalityId("2281").withNamespace("ns");
		final var expires = OffsetDateTime.now().plusDays(30);
		final var notification = Notification.create()
			.withOwnerId("jane01doe")
			.withCreatedBy("john02doe")
			.withType("CREATE")
			.withSubType("ERRAND")
			.withDescription("desc")
			.withContent("content")
			.withAcknowledged(true);

		final var result = NotificationMapper.toEntity(notification, errand, expires);

		assertThat(result).isNotNull();
		assertThat(result.getErrandEntity()).isSameAs(errand);
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("ns");
		assertThat(result.getOwnerId()).isEqualTo("jane01doe");
		assertThat(result.getCreatedBy()).isEqualTo("john02doe");
		assertThat(result.getType()).isEqualTo(NotificationType.CREATE);
		assertThat(result.getSubType()).isEqualTo(NotificationSubType.ERRAND);
		assertThat(result.getDescription()).isEqualTo("desc");
		assertThat(result.getContent()).isEqualTo("content");
		assertThat(result.isAcknowledged()).isTrue();
		assertThat(result.getExpires()).isEqualTo(expires);
		assertThat(result.getId()).isNull();
		assertThat(result.getCreated()).isNull();
		assertThat(result.getModified()).isNull();
	}

	@Test
	void toEntity_acknowledgedNullDefaultsFalse() {
		final var errand = ErrandEntity.create().withId("eid").withMunicipalityId("2281").withNamespace("ns");
		final var notification = Notification.create()
			.withOwnerId("o")
			.withType("CREATE")
			.withDescription("d");

		final var result = NotificationMapper.toEntity(notification, errand, OffsetDateTime.now());

		assertThat(result.isAcknowledged()).isFalse();
	}

	@Test
	void toEntity_nullReturnsNull() {
		assertThat(NotificationMapper.toEntity(null, ErrandEntity.create(), OffsetDateTime.now())).isNull();
	}

	@Test
	void toDto_maps() {
		final var errand = ErrandEntity.create().withId("eid");
		final var entity = NotificationEntity.create()
			.withId("nid")
			.withErrandEntity(errand)
			.withOwnerId("jane01doe")
			.withCreatedBy("john02doe")
			.withType(NotificationType.UPDATE)
			.withSubType(NotificationSubType.DECISION)
			.withDescription("desc")
			.withContent("content")
			.withAcknowledged(true)
			.withExpires(OffsetDateTime.parse("2026-12-31T00:00:00Z"))
			.withCreated(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
			.withModified(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

		final var result = NotificationMapper.toDto(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("nid");
		assertThat(result.getErrandId()).isEqualTo("eid");
		assertThat(result.getOwnerId()).isEqualTo("jane01doe");
		assertThat(result.getCreatedBy()).isEqualTo("john02doe");
		assertThat(result.getType()).isEqualTo("UPDATE");
		assertThat(result.getSubType()).isEqualTo("DECISION");
		assertThat(result.getDescription()).isEqualTo("desc");
		assertThat(result.getContent()).isEqualTo("content");
		assertThat(result.getAcknowledged()).isTrue();
		assertThat(result.getExpires()).isEqualTo(OffsetDateTime.parse("2026-12-31T00:00:00Z"));
		assertThat(result.getCreated()).isEqualTo(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
		assertThat(result.getModified()).isEqualTo(OffsetDateTime.parse("2026-01-02T00:00:00Z"));
	}

	@Test
	void toDto_nullEntityReturnsNull() {
		assertThat(NotificationMapper.toDto(null)).isNull();
	}

	@Test
	void toDto_handlesNullErrandAndEnums() {
		final var entity = NotificationEntity.create().withId("nid");
		final var result = NotificationMapper.toDto(entity);

		assertThat(result.getErrandId()).isNull();
		assertThat(result.getType()).isNull();
		assertThat(result.getSubType()).isNull();
	}

	@Test
	void applyPatch_setsAllProvidedFields() {
		final var entity = NotificationEntity.create()
			.withType(NotificationType.CREATE)
			.withSubType(NotificationSubType.ERRAND)
			.withDescription("old")
			.withContent("oldc")
			.withAcknowledged(false);
		final var patch = Notification.create()
			.withType("UPDATE")
			.withSubType("DECISION")
			.withDescription("new")
			.withContent("newc")
			.withAcknowledged(true);

		NotificationMapper.applyPatch(entity, patch);

		assertThat(entity.getType()).isEqualTo(NotificationType.UPDATE);
		assertThat(entity.getSubType()).isEqualTo(NotificationSubType.DECISION);
		assertThat(entity.getDescription()).isEqualTo("new");
		assertThat(entity.getContent()).isEqualTo("newc");
		assertThat(entity.isAcknowledged()).isTrue();
	}

	@Test
	void applyPatch_nullFieldsLeaveTargetUnchanged() {
		final var entity = NotificationEntity.create()
			.withType(NotificationType.CREATE)
			.withDescription("keep")
			.withAcknowledged(true);
		final var patch = Notification.create();

		NotificationMapper.applyPatch(entity, patch);

		assertThat(entity.getType()).isEqualTo(NotificationType.CREATE);
		assertThat(entity.getDescription()).isEqualTo("keep");
		assertThat(entity.isAcknowledged()).isTrue();
	}

	@Test
	void applyPatch_acknowledgedFalseExplicitlyClears() {
		final var entity = NotificationEntity.create().withAcknowledged(true);
		final var patch = Notification.create().withAcknowledged(false);

		NotificationMapper.applyPatch(entity, patch);

		assertThat(entity.isAcknowledged()).isFalse();
	}

	@Test
	void applyPatch_nullTargetIsNoop() {
		NotificationMapper.applyPatch(null, Notification.create());
	}

	@Test
	void applyPatch_nullPatchIsNoop() {
		final var entity = NotificationEntity.create().withDescription("keep");
		NotificationMapper.applyPatch(entity, null);
		assertThat(entity.getDescription()).isEqualTo("keep");
	}
}
