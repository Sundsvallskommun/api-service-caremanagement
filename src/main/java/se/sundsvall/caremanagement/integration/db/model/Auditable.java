package se.sundsvall.caremanagement.integration.db.model;

import java.time.OffsetDateTime;

/**
 * Marker-interface for entities that want created/modified timestamps handled by {@link AuditableListener}.
 */
public interface Auditable {

	void setCreated(OffsetDateTime created);

	void setModified(OffsetDateTime modified);
}
