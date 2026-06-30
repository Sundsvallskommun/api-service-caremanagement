package se.sundsvall.caremanagement.types.financialassistance.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;

/**
 * Removes the financial assistance data (and its owned {@code errand_fa_*} value rows, which cascade) when the errand
 * is
 * deleted. Deletes via the loaded entity so Hibernate cascades the {@code @ElementCollection} tables. Errands of other
 * types have no row here, so the listener is a no-op for them.
 */
@Component
class FinancialAssistanceErrandDeletedListener {

	private final FinancialAssistanceRepository repository;

	FinancialAssistanceErrandDeletedListener(final FinancialAssistanceRepository repository) {
		this.repository = repository;
	}

	@ApplicationModuleListener
	void on(final ErrandDeleted event) {
		repository.findByErrandId(event.errandId()).ifPresent(repository::delete);
	}
}
