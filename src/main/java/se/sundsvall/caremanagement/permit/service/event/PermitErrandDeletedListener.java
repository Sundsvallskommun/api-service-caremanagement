package se.sundsvall.caremanagement.permit.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.permit.integration.db.PermitRepository;

/**
 * Removes every permit row tied to a deleted errand. Mirrors the decisions module's cleanup —
 * {@code @ApplicationModuleListener} runs asynchronously in a fresh transaction after the originating delete commits,
 * with the event durably staged in Spring Modulith's outbox in between, keeping the modules loosely coupled.
 */
@Component
class PermitErrandDeletedListener {

	private final PermitRepository permitRepository;

	PermitErrandDeletedListener(final PermitRepository permitRepository) {
		this.permitRepository = permitRepository;
	}

	@ApplicationModuleListener
	void on(final ErrandDeleted event) {
		permitRepository.deleteByErrandId(event.errandId());
	}
}
