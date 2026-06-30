package se.sundsvall.caremanagement.notifications.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationSubType;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationMapperTest {

	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	// ---------------------------------------------------------------------
	// toEntity
	// ---------------------------------------------------------------------

	@Test
	void toEntityMapsAllFields() {
		final var expires = FIXED_TIMESTAMP.plusDays(30);
		final var notification = Notification.create()
			.withId("ignored-id")
			.withErrandId("ignored-errand-id")
			.withOwnerId("jane01doe")
			.withCreatedBy("john02doe")
			.withType("CREATE")
			.withSubType("ERRAND")
			.withDescription("New errand assigned to you")
			.withContent("A longer body of text")
			.withAcknowledged(true);

		final var entity = NotificationMapper.toEntity(notification, "2281", "FINANCIAL_ASSISTANCE", "errand-123", expires);

		assertThat(entity).isNotNull();
		// path/argument-supplied fields, not from the source notification
		assertThat(entity.getErrandId()).isEqualTo("errand-123");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(entity.getExpires()).isEqualTo(expires);
		// fields copied from the source notification
		assertThat(entity.getOwnerId()).isEqualTo("jane01doe");
		assertThat(entity.getCreatedBy()).isEqualTo("john02doe");
		assertThat(entity.getType()).isEqualTo(NotificationType.CREATE);
		assertThat(entity.getSubType()).isEqualTo(NotificationSubType.ERRAND);
		assertThat(entity.getDescription()).isEqualTo("New errand assigned to you");
		assertThat(entity.getContent()).isEqualTo("A longer body of text");
		assertThat(entity.isAcknowledged()).isTrue();
		// id is server-assigned, never carried over from the dto
		assertThat(entity.getId()).isNull();
	}

	@Test
	void toEntityAcknowledgedDefaultsToFalseWhenNull() {
		final var notification = Notification.create()
			.withType("UPDATE")
			.withSubType("DECISION")
			.withDescription("desc")
			.withAcknowledged(null);

		final var entity = NotificationMapper.toEntity(notification, "2281", "ns", "errand-1", FIXED_TIMESTAMP);

		assertThat(entity).isNotNull();
		assertThat(entity.isAcknowledged()).isFalse();
	}

	@Test
	void toEntityAcknowledgedFalseStaysFalse() {
		final var notification = Notification.create()
			.withType("DELETE")
			.withDescription("desc")
			.withAcknowledged(false);

		final var entity = NotificationMapper.toEntity(notification, "2281", "ns", "errand-1", FIXED_TIMESTAMP);

		assertThat(entity.isAcknowledged()).isFalse();
	}

	@Test
	void toEntityNullTypeAndSubTypeYieldNullEnums() {
		final var notification = Notification.create()
			.withType(null)
			.withSubType(null)
			.withDescription("desc");

		final var entity = NotificationMapper.toEntity(notification, "2281", "ns", "errand-1", FIXED_TIMESTAMP);

		assertThat(entity).isNotNull();
		assertThat(entity.getType()).isNull();
		assertThat(entity.getSubType()).isNull();
	}

	@Test
	void toEntityNullNotificationReturnsNull() {
		assertThat(NotificationMapper.toEntity(null, "2281", "ns", "errand-1", FIXED_TIMESTAMP)).isNull();
	}

	@Test
	void toEntityInvalidTypeThrows() {
		final var notification = Notification.create()
			.withType("NOT_A_TYPE")
			.withDescription("desc");

		assertThatThrownBy(() -> NotificationMapper.toEntity(notification, "2281", "ns", "errand-1", FIXED_TIMESTAMP))
			.isInstanceOf(IllegalArgumentException.class);
	}

	// ---------------------------------------------------------------------
	// toDto
	// ---------------------------------------------------------------------

	@Test
	void toDtoMapsAllFieldsIncludingDisplayNames() {
		final var created = FIXED_TIMESTAMP.minusDays(1);
		final var modified = FIXED_TIMESTAMP;
		final var expires = FIXED_TIMESTAMP.plusDays(30);
		final var entity = NotificationEntity.create()
			.withId("notif-id")
			.withErrandId("errand-123")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withOwnerId("jane01doe")
			.withCreatedBy("john02doe")
			.withType(NotificationType.CREATE)
			.withSubType(NotificationSubType.ERRAND)
			.withDescription("New errand assigned to you")
			.withContent("A longer body of text")
			.withAcknowledged(true)
			.withExpires(expires)
			.withCreated(created)
			.withModified(modified);

		final var dto = NotificationMapper.toDto(entity);

		assertThat(dto).isNotNull();
		assertThat(dto.getId()).isEqualTo("notif-id");
		assertThat(dto.getErrandId()).isEqualTo("errand-123");
		assertThat(dto.getOwnerId()).isEqualTo("jane01doe");
		assertThat(dto.getCreatedBy()).isEqualTo("john02doe");
		assertThat(dto.getType()).isEqualTo("CREATE");
		assertThat(dto.getTypeDisplayName()).isEqualTo("Skapad");
		assertThat(dto.getSubType()).isEqualTo("ERRAND");
		assertThat(dto.getSubTypeDisplayName()).isEqualTo("Ärende");
		assertThat(dto.getDescription()).isEqualTo("New errand assigned to you");
		assertThat(dto.getContent()).isEqualTo("A longer body of text");
		assertThat(dto.getAcknowledged()).isTrue();
		assertThat(dto.getExpires()).isEqualTo(expires);
		assertThat(dto.getCreated()).isEqualTo(created);
		assertThat(dto.getModified()).isEqualTo(modified);
	}

	@Test
	void toDtoMapsEveryTypeDisplayName() {
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withType(NotificationType.CREATE)).getTypeDisplayName()).isEqualTo("Skapad");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withType(NotificationType.UPDATE)).getTypeDisplayName()).isEqualTo("Uppdaterad");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withType(NotificationType.DELETE)).getTypeDisplayName()).isEqualTo("Borttagen");
	}

	@Test
	void toDtoMapsEverySubTypeDisplayName() {
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.ERRAND)).getSubTypeDisplayName()).isEqualTo("Ärende");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.DECISION)).getSubTypeDisplayName()).isEqualTo("Beslut");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.ATTACHMENT)).getSubTypeDisplayName()).isEqualTo("Bilaga");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.STAKEHOLDER)).getSubTypeDisplayName()).isEqualTo("Intressent");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.PARAMETER)).getSubTypeDisplayName()).isEqualTo("Parameter");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.MESSAGE)).getSubTypeDisplayName()).isEqualTo("Meddelande");
		assertThat(NotificationMapper.toDto(NotificationEntity.create().withSubType(NotificationSubType.SYSTEM)).getSubTypeDisplayName()).isEqualTo("System");
	}

	@Test
	void toDtoNullTypeAndSubTypeYieldNullStringsAndDisplayNames() {
		final var entity = NotificationEntity.create()
			.withType(null)
			.withSubType(null)
			.withDescription("desc");

		final var dto = NotificationMapper.toDto(entity);

		assertThat(dto).isNotNull();
		assertThat(dto.getType()).isNull();
		assertThat(dto.getTypeDisplayName()).isNull();
		assertThat(dto.getSubType()).isNull();
		assertThat(dto.getSubTypeDisplayName()).isNull();
	}

	@Test
	void toDtoAcknowledgedFalseMapped() {
		final var dto = NotificationMapper.toDto(NotificationEntity.create().withAcknowledged(false));

		assertThat(dto.getAcknowledged()).isFalse();
	}

	@Test
	void toDtoNullEntityReturnsNull() {
		assertThat(NotificationMapper.toDto(null)).isNull();
	}

	// ---------------------------------------------------------------------
	// applyPatch
	// ---------------------------------------------------------------------

	@Test
	void applyPatchUpdatesOnlyNonNullFields() {
		final var target = NotificationEntity.create()
			.withType(NotificationType.CREATE)
			.withSubType(NotificationSubType.ERRAND)
			.withDescription("old description")
			.withContent("old content")
			.withAcknowledged(false);

		final var patch = Notification.create()
			.withType("UPDATE")
			.withSubType("DECISION")
			.withDescription("new description")
			.withContent("new content")
			.withAcknowledged(true);

		NotificationMapper.applyPatch(target, patch);

		assertThat(target.getType()).isEqualTo(NotificationType.UPDATE);
		assertThat(target.getSubType()).isEqualTo(NotificationSubType.DECISION);
		assertThat(target.getDescription()).isEqualTo("new description");
		assertThat(target.getContent()).isEqualTo("new content");
		assertThat(target.isAcknowledged()).isTrue();
	}

	@Test
	void applyPatchLeavesUnsetFieldsUnchanged() {
		final var target = NotificationEntity.create()
			.withType(NotificationType.CREATE)
			.withSubType(NotificationSubType.ERRAND)
			.withDescription("kept description")
			.withContent("kept content")
			.withAcknowledged(true);

		// every patch field is null -> nothing should change
		final var patch = Notification.create();

		NotificationMapper.applyPatch(target, patch);

		assertThat(target.getType()).isEqualTo(NotificationType.CREATE);
		assertThat(target.getSubType()).isEqualTo(NotificationSubType.ERRAND);
		assertThat(target.getDescription()).isEqualTo("kept description");
		assertThat(target.getContent()).isEqualTo("kept content");
		assertThat(target.isAcknowledged()).isTrue();
	}

	@Test
	void applyPatchAcknowledgedFalseOverwritesTrue() {
		final var target = NotificationEntity.create().withAcknowledged(true);
		final var patch = Notification.create().withAcknowledged(false);

		NotificationMapper.applyPatch(target, patch);

		assertThat(target.isAcknowledged()).isFalse();
	}

	@Test
	void applyPatchNullTargetIsNoOp() {
		// must not throw
		NotificationMapper.applyPatch(null, Notification.create().withDescription("x"));
	}

	@Test
	void applyPatchNullPatchLeavesTargetUntouched() {
		final var target = NotificationEntity.create().withDescription("kept");

		NotificationMapper.applyPatch(target, null);

		assertThat(target.getDescription()).isEqualTo("kept");
	}

	@Test
	void applyPatchInvalidTypeThrows() {
		final var target = NotificationEntity.create();
		final var patch = Notification.create().withType("BOGUS");

		assertThatThrownBy(() -> NotificationMapper.applyPatch(target, patch))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
