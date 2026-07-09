package se.sundsvall.caremanagement.decisions.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.decisions.integration.db.DecisionRepository;

/**
 * Removes every decision row tied to a deleted errand. {@code @ApplicationModuleListener}
 * runs asynchronously in a fresh transaction after the originating delete commits, with the
 * event durably staged in Spring Modulith's outbox in between — replaces the legacy DB-level
 * cascade and keeps the modules loosely coupled.
 */
@Component
class DecisionErrandDeletedListener {

	private final DecisionRepository decisionRepository;

	DecisionErrandDeletedListener(final DecisionRepository decisionRepository) {
		this.decisionRepository = decisionRepository;
	}

	@ApplicationModuleListener
	void on(final ErrandDeleted event) {
		decisionRepository.deleteByErrandId(event.errandId());
	}
}
