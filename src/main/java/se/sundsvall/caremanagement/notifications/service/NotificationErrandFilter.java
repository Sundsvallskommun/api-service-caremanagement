package se.sundsvall.caremanagement.notifications.service;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.model.CoCaseworkerEntity;
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
 *
 * <p>
 * When narrowed to a recipient, the recipient match also widens to co-caseworker: an errand shows up for a
 * medhandläggare's "errands with unread/unhandled notifications" filter the same way it does for the errand's
 * direct owner (see {@code cocaseworkers.integration.db.model.CoCaseworkerEntity}), since the underlying
 * notification is the same shared row either recipient would see.
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
				predicates.add(cb.or(
					cb.equal(notification.get("ownerId"), ownerId),
					cb.exists(coCaseworkerExists(subquery, cb, notification, municipalityId, namespace, ownerId))));
			}

			subquery.select(notification.get("id")).where(predicates.toArray(Predicate[]::new));
			return cb.exists(subquery);
		};
	}

	/**
	 * Correlated {@code EXISTS} matching a co-caseworker row for {@code ownerId} on the same errand as the enclosing
	 * notification subquery — nested under it (rather than the outer query) so the correlation to
	 * {@code notification.errandId} is well-formed. This is the join that lets a medhandläggare's errand-level
	 * notification filters see what the errand's direct owner sees.
	 */
	private static Subquery<String> coCaseworkerExists(final AbstractQuery<?> enclosing, final CriteriaBuilder cb,
		final Path<?> notification, final String municipalityId, final String namespace, final String ownerId) {

		final var subquery = enclosing.subquery(String.class);
		final var coCaseworker = subquery.from(CoCaseworkerEntity.class);

		final var predicates = new ArrayList<Predicate>();
		predicates.add(cb.equal(coCaseworker.get("errandId"), notification.get("errandId")));
		predicates.add(cb.equal(coCaseworker.get("municipalityId"), municipalityId));
		predicates.add(cb.equal(coCaseworker.get("namespace"), namespace));
		predicates.add(cb.equal(coCaseworker.get("userId"), ownerId));

		subquery.select(coCaseworker.get("id")).where(predicates.toArray(Predicate[]::new));
		return subquery;
	}
}
