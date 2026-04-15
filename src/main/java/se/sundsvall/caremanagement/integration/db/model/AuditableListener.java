package se.sundsvall.caremanagement.integration.db.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Shared {@code @PrePersist}/{@code @PreUpdate} callback for all {@link Auditable} entities. Attach via
 * {@code @EntityListeners(AuditableListener.class)}.
 */
public class AuditableListener {

	@PrePersist
	void onCreate(final Object o) {
		if (o instanceof final Auditable a) {
			a.setCreated(now(systemDefault()).truncatedTo(MILLIS));
		}
	}

	@PreUpdate
	void onUpdate(final Object o) {
		if (o instanceof final Auditable a) {
			a.setModified(now(systemDefault()).truncatedTo(MILLIS));
		}
	}
}
