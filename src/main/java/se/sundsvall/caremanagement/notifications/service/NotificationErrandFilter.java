package se.sundsvall.caremanagement.notifications.service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.core.service.ErrandNotificationFilter;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;

import static org.springframework.util.StringUtils.hasText;

/**
 * Builds the {@code EXISTS} subqueries against the notification table for {@link ErrandNotificationFilter}. Each
 * subquery correlates a notification's {@code errandId} to the errand row and keeps only the rows still false on the
 * flag asked about - {@code acknowledged} (unread) or {@code handled} (not yet dealt with) - in the same namespace and
 * municipality, optionally narrowed to one recipient. The indexes
 * {@code idx_notification_mid_ns_owner_id_acknowledged} and {@code idx_notification_mid_ns_owner_id_handled} carry the
 * respective lookups.
 */
@Component
class NotificationErrandFilter implements ErrandNotificationFilter {

	private static final String ACKNOWLEDGED = "acknowledged";
	private static final String HANDLED = "handled";

	@Override
	public Specification<ErrandEntity> hasUnacknowledgedNotifications(final String municipalityId, final String namespace, final String ownerId) {
		return hasNotificationsWithFalseFlag(ACKNOWLEDGED, municipalityId, namespace, ownerId);
	}

	@Override
	public Specification<ErrandEntity> hasUnhandledNotifications(final String municipalityId, final String namespace, final String ownerId) {
		return hasNotificationsWithFalseFlag(HANDLED, municipalityId, namespace, ownerId);
	}

	private static Specification<ErrandEntity> hasNotificationsWithFalseFlag(final String flag, final String municipalityId,
		final String namespace, final String ownerId) {

		return (root, query, cb) -> {
			final var subquery = query.subquery(String.class);
			final var notification = subquery.from(NotificationEntity.class);

			final var predicates = new ArrayList<Predicate>();
			predicates.add(cb.equal(notification.get("errandId"), root.get("id")));
			predicates.add(cb.equal(notification.get("municipalityId"), municipalityId));
			predicates.add(cb.equal(notification.get("namespace"), namespace));
			predicates.add(cb.isFalse(notification.get(flag)));
			if (hasText(ownerId)) {
				predicates.add(cb.equal(notification.get("ownerId"), ownerId));
			}

			subquery.select(notification.get("id")).where(predicates.toArray(Predicate[]::new));
			return cb.exists(subquery);
		};
	}
}
