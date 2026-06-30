package se.sundsvall.caremanagement.formsnapshot.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.formsnapshot.integration.db.FormSnapshotRepository;

/**
 * Removes the form snapshot when its errand is deleted, so the snapshot's retention follows the case (gallring). The DB
 * {@code on delete cascade} is the backstop; this keeps the module's store consistent. Errands without a snapshot are a
 * no-op.
 */
@Component
class FormSnapshotErrandDeletedListener {

	private final FormSnapshotRepository repository;

	FormSnapshotErrandDeletedListener(final FormSnapshotRepository repository) {
		this.repository = repository;
	}

	@ApplicationModuleListener
	void on(final ErrandDeleted event) {
		repository.deleteByErrandId(event.errandId());
	}
}
