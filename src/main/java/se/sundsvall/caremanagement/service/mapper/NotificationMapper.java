package se.sundsvall.caremanagement.service.mapper;

import java.time.OffsetDateTime;
import se.sundsvall.caremanagement.api.model.Notification;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationSubType;
import se.sundsvall.caremanagement.integration.db.model.NotificationType;

import static java.util.Optional.ofNullable;

public final class NotificationMapper {

	private NotificationMapper() {}

	public static NotificationEntity toEntity(final Notification notification, final ErrandEntity errandEntity, final OffsetDateTime expires) {
		return ofNullable(notification)
			.map(source -> NotificationEntity.create()
				.withErrandEntity(errandEntity)
				.withMunicipalityId(errandEntity.getMunicipalityId())
				.withNamespace(errandEntity.getNamespace())
				.withOwnerId(source.getOwnerId())
				.withCreatedBy(source.getCreatedBy())
				.withType(parseType(source.getType()))
				.withSubType(parseSubType(source.getSubType()))
				.withDescription(source.getDescription())
				.withContent(source.getContent())
				.withAcknowledged(Boolean.TRUE.equals(source.getAcknowledged()))
				.withExpires(expires))
			.orElse(null);
	}

	public static Notification toDto(final NotificationEntity entity) {
		return ofNullable(entity)
			.map(source -> Notification.create()
				.withId(source.getId())
				.withErrandId(ofNullable(source.getErrandEntity()).map(ErrandEntity::getId).orElse(null))
				.withOwnerId(source.getOwnerId())
				.withCreatedBy(source.getCreatedBy())
				.withType(ofNullable(source.getType()).map(Enum::name).orElse(null))
				.withSubType(ofNullable(source.getSubType()).map(Enum::name).orElse(null))
				.withDescription(source.getDescription())
				.withContent(source.getContent())
				.withAcknowledged(source.isAcknowledged())
				.withExpires(source.getExpires())
				.withCreated(source.getCreated())
				.withModified(source.getModified()))
			.orElse(null);
	}

	public static void applyPatch(final NotificationEntity target, final Notification patch) {
		if ((target == null) || (patch == null)) {
			return;
		}
		ofNullable(patch.getType()).map(NotificationMapper::parseType).ifPresent(target::setType);
		ofNullable(patch.getSubType()).map(NotificationMapper::parseSubType).ifPresent(target::setSubType);
		ofNullable(patch.getDescription()).ifPresent(target::setDescription);
		ofNullable(patch.getContent()).ifPresent(target::setContent);
		ofNullable(patch.getAcknowledged()).ifPresent(target::setAcknowledged);
	}

	private static NotificationType parseType(final String value) {
		return ofNullable(value).map(NotificationType::valueOf).orElse(null);
	}

	private static NotificationSubType parseSubType(final String value) {
		return ofNullable(value).map(NotificationSubType::valueOf).orElse(null);
	}
}
