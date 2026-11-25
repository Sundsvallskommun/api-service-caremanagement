package se.sundsvall.caremanagement.core.service;

import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

/**
 * Dependency-inversion seam letting the core errand query restrict to errands that carry unacknowledged or unhandled
 * notifications, without core depending on the notifications module. The notifications module owns the notification
 * table and supplies
 * the {@link Specification} (a correlated {@code EXISTS} subquery), which core composes with the rest of the errand
 * filter. The arrow points notifications → core, preserving the module boundary.
 */
public interface ErrandNotificationFilter {

	/**
	 * A specification matching errands that have at least one unacknowledged notification in the given namespace and
	 * municipality. When {@code ownerId} is non-blank the match is scoped to notifications addressed to that recipient
	 * (the caseworker), so a caseworker can find the errands where they have something unread.
	 */
	Specification<ErrandEntity> hasUnacknowledgedNotifications(String municipalityId, String namespace, String ownerId);

	/**
	 * A specification matching errands that have at least one notification not yet marked handled in the given namespace
	 * and municipality. Handled is a state of its own: a caseworker may have read a message without having dealt with it,
	 * so this is the filter behind "errands with unhandled messages", where
	 * {@link #hasUnacknowledgedNotifications(String, String, String)} answers "errands with unread messages". When
	 * {@code ownerId} is non-blank the match is scoped to notifications addressed to that recipient.
	 */
	Specification<ErrandEntity> hasUnhandledNotifications(String municipalityId, String namespace, String ownerId);
}
