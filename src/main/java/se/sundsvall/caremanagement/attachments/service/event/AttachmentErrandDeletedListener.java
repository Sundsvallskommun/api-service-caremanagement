package se.sundsvall.caremanagement.attachments.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;

/**
 * Removes every attachment row tied to a deleted errand. {@code @ApplicationModuleListener}
 * runs asynchronously in a fresh transaction after the originating delete commits, with the
 * event durably staged in Spring Modulith's outbox in between — replaces the legacy DB-level
 * cascade and keeps the modules loosely coupled.
 *
 * <p>
 * Attachments are loaded and removed via {@code deleteAll(entities)} rather than a derived bulk
 * {@code delete ... where errand_id = ?}: {@code deleteAll} still removes each entity through the persistence context
 * (not a bulk SQL delete), so the {@code cascade = ALL} on
 * {@link se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity#getAttachmentData()}
 * removes the child {@code attachment_data} blob too. A bulk delete of the parent {@code attachment} rows would orphan
 * every {@code attachment_data} longblob, because the only DB-level {@code ON DELETE CASCADE} runs from
 * {@code attachment_data} to {@code attachment}, not the other way around.
 * </p>
 */
@Component
class AttachmentErrandDeletedListener {

	private final AttachmentRepository repository;

	AttachmentErrandDeletedListener(final AttachmentRepository repository) {
		this.repository = repository;
	}

	@ApplicationModuleListener
	void on(final ErrandDeleted event) {
		repository.deleteAll(repository.findByErrandId(event.errandId()));
	}
}
