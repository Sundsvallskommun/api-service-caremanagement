package se.sundsvall.caremanagement.stakeholders.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.stakeholders.integration.db.StakeholderRepository;

/**
 * Removes every stakeholder row tied to a deleted errand. {@code @ApplicationModuleListener}
 * runs asynchronously in a fresh transaction after the originating delete commits, with the
 * event durably staged in Spring Modulith's outbox in between — replaces the legacy DB-level
 * cascade and keeps the modules loosely coupled.
 */
@Component
class StakeholderErrandDeletedListener {

	private final StakeholderRepository repository;

	StakeholderErrandDeletedListener(final StakeholderRepository repository) {
		this.repository = repository;
	}

	@ApplicationModuleListener
	void on(final ErrandDeleted event) {
		repository.deleteByErrandId(event.errandId());
	}
}
