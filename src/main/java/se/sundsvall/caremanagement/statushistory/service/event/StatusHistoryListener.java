package se.sundsvall.caremanagement.statushistory.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.model.StatusHistoryEntity;

/**
 * Persists every {@link ErrandStatusChanged} as a row in {@code errand_status_history}.
 *
 * {@code @ApplicationModuleListener} runs asynchronously after the originating transaction
 * commits, with the event durably persisted to the Modulith outbox in between.
 */
@Component
class StatusHistoryListener {

	private final StatusHistoryRepository repository;

	StatusHistoryListener(final StatusHistoryRepository repository) {
		this.repository = repository;
	}

	@ApplicationModuleListener
	void on(final ErrandStatusChanged event) {
		repository.save(StatusHistoryEntity.create()
			.withErrandId(event.errandId())
			.withFromStatus(event.fromStatus())
			.withToStatus(event.toStatus())
			.withChangedBy(event.changedBy())
			.withChangedAt(event.timestamp()));
	}
}
