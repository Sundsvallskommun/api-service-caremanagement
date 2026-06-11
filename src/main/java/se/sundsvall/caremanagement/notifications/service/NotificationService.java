package se.sundsvall.caremanagement.notifications.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.integration.db.NotificationRepository;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.notifications.service.mapper.NotificationMapper;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class NotificationService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String NOTIFICATION_NOT_FOUND_MESSAGE = "No notification with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final NotificationRepository notificationRepository;
	private final NotificationProperties properties;

	NotificationService(final ErrandRepository errandRepository, final NotificationRepository notificationRepository, final NotificationProperties properties) {
		this.errandRepository = errandRepository;
		this.notificationRepository = notificationRepository;
		this.properties = properties;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Notification notification) {
		ensureErrandExists(municipalityId, namespace, errandId);
		final var expires = OffsetDateTime.now(ZoneId.systemDefault()).plus(properties.ttl());
		final var entity = NotificationMapper.toEntity(notification, municipalityId, namespace, errandId, expires);
		if (selfCreated(notification)) {
			entity.setAcknowledged(true);
		}
		return notificationRepository.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public Notification read(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		return NotificationMapper.toDto(findNotification(municipalityId, namespace, errandId, notificationId));
	}

	@Transactional(readOnly = true)
	public List<Notification> readAllByErrand(final String municipalityId, final String namespace, final String errandId, final Sort sort) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandId(namespace, municipalityId, errandId, sort).stream()
			.map(NotificationMapper::toDto)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<Notification> readAllByOwner(final String municipalityId, final String namespace, final String ownerId, final Sort sort) {
		return notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(namespace, municipalityId, ownerId, sort).stream()
			.map(NotificationMapper::toDto)
			.toList();
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String notificationId, final Notification patch) {
		final var entity = findNotification(municipalityId, namespace, errandId, notificationId);
		NotificationMapper.applyPatch(entity, patch);
		notificationRepository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		final var entity = findNotification(municipalityId, namespace, errandId, notificationId);
		notificationRepository.delete(entity);
	}

	public int acknowledgeAll(final String municipalityId, final String namespace, final String errandId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return notificationRepository.acknowledgeAllByErrand(namespace, municipalityId, errandId);
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private NotificationEntity findNotification(final String municipalityId, final String namespace, final String errandId, final String notificationId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandId(notificationId, namespace, municipalityId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOTIFICATION_NOT_FOUND_MESSAGE.formatted(notificationId, errandId, namespace, municipalityId)));
	}

	private static boolean selfCreated(final Notification notification) {
		final var createdBy = notification.getCreatedBy();
		final var ownerId = notification.getOwnerId();
		return (createdBy != null) && createdBy.equals(ownerId);
	}
}
